package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.AutomationJob;
import com.hospital.followup.domain.MessageTriggerExecution;
import com.hospital.followup.domain.ReminderTask;
import com.hospital.followup.domain.enums.AutomationJobStatus;
import com.hospital.followup.domain.enums.AutomationJobType;
import com.hospital.followup.domain.enums.ReminderTaskStatus;
import com.hospital.followup.dto.admin.AutomationJobView;
import com.hospital.followup.dto.worker.WorkerAutomationJobView;
import com.hospital.followup.dto.worker.WorkerClaimJobRequest;
import com.hospital.followup.dto.worker.WorkerJobResultRequest;
import com.hospital.followup.repository.AutomationJobRepository;
import com.hospital.followup.repository.MessageTriggerExecutionRepository;
import com.hospital.followup.repository.ReminderTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationJobService {

    private static final int MAX_SHORT_TEXT_LENGTH = 255;
    private static final int MAX_COMMAND_LINE_LENGTH = 500;
    private static final int MAX_LOG_LENGTH = 20000;
    private static final int MAX_RESULT_JSON_LENGTH = 20000;

    private final AutomationJobRepository automationJobRepository;
    private final ReminderTaskRepository reminderTaskRepository;
    private final MessageTriggerExecutionRepository messageTriggerExecutionRepository;
    private final AutomationJobNumberGenerator automationJobNumberGenerator;
    private final ObjectMapper objectMapper;

    public AutomationJobService(
        AutomationJobRepository automationJobRepository,
        ReminderTaskRepository reminderTaskRepository,
        MessageTriggerExecutionRepository messageTriggerExecutionRepository,
        AutomationJobNumberGenerator automationJobNumberGenerator,
        ObjectMapper objectMapper
    ) {
        this.automationJobRepository = automationJobRepository;
        this.reminderTaskRepository = reminderTaskRepository;
        this.messageTriggerExecutionRepository = messageTriggerExecutionRepository;
        this.automationJobNumberGenerator = automationJobNumberGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AutomationJob createWechatReminderJob(
        ReminderTask reminderTask,
        String taskNo,
        String targetConversation,
        String content,
        int countdownSeconds,
        LocalDateTime plannedAt
    ) {
        return createWechatReminderJob(
            reminderTask, taskNo, targetConversation, content, countdownSeconds, plannedAt, null
        );
    }

    /**
     * 支持随消息一起发送一张图片（通常是问卷二维码）。
     * qrImagePath 必须是【worker 执行机器上可直接读到】的绝对路径；
     * 留空则退化为只发文本，和旧行为一致。
     */
    @Transactional
    public AutomationJob createWechatReminderJob(
        ReminderTask reminderTask,
        String taskNo,
        String targetConversation,
        String content,
        int countdownSeconds,
        LocalDateTime plannedAt,
        String qrImagePath
    ) {
        AutomationJob job = new AutomationJob();
        job.setJobNo(automationJobNumberGenerator.nextNo());
        job.setJobType(AutomationJobType.WECHAT_RPA_SEND);
        job.setBizType("REMINDER_TASK");
        job.setBizId(reminderTask.getId());
        job.setChannel("WECHAT_RPA");

        Map<String, Object> payload = new HashMap<>();
        payload.put("taskNo", taskNo);
        payload.put("reminderTaskId", reminderTask.getId());
        payload.put("targetConversation", targetConversation);
        payload.put("content", content);
        payload.put("countdownSeconds", countdownSeconds);
        if (qrImagePath != null && !qrImagePath.isBlank()) {
            payload.put("qrImagePath", qrImagePath);
        }
        job.setPayloadJson(writeJson(payload));

        job.setStatus(AutomationJobStatus.QUEUED);
        job.setPlannedAt(plannedAt);
        job.setRetryCount(0);
        job.setExecutionLog(appendLog("", "自动化任务已创建，等待 worker 领取"));
        return automationJobRepository.save(job);
    }

    @Transactional
    public AutomationJob createMessageTriggerJob(
        MessageTriggerExecution execution,
        String targetConversation,
        String content,
        int countdownSeconds,
        LocalDateTime plannedAt,
        List<String> imagePaths
    ) {
        AutomationJob job = new AutomationJob();
        job.setJobNo(automationJobNumberGenerator.nextNo());
        job.setJobType(AutomationJobType.WECHAT_RPA_SEND);
        job.setBizType("MESSAGE_TRIGGER_EXECUTION");
        job.setBizId(execution.getId());
        job.setChannel("WECHAT_RPA");

        Map<String, Object> payload = new HashMap<>();
        payload.put("ruleCode", execution.getRule().getRuleCode());
        payload.put("executionId", execution.getId());
        payload.put("patientId", execution.getPatient().getPatientId());
        payload.put("targetConversation", targetConversation);
        payload.put("content", content == null ? "" : content);
        payload.put("countdownSeconds", countdownSeconds);
        if (imagePaths != null && !imagePaths.isEmpty()) {
            payload.put("imagePaths", imagePaths);
        }

        job.setPayloadJson(writeJson(payload));
        job.setStatus(AutomationJobStatus.QUEUED);
        job.setPlannedAt(plannedAt);
        job.setRetryCount(0);
        job.setExecutionLog(appendLog("", "消息规则自动化任务已创建，等待 worker 领取"));
        return automationJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<AutomationJobView> listRecentJobs() {
        return automationJobRepository.findTop100ByOrderByCreatedAtDesc()
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public WorkerAutomationJobView claimNextJob(WorkerClaimJobRequest request) {
        AutomationJob job = automationJobRepository
            .findFirstByStatusAndJobTypeAndPlannedAtLessThanEqualOrderByPlannedAtAscCreatedAtAsc(
                AutomationJobStatus.QUEUED,
                request.jobType(),
                LocalDateTime.now()
            )
            .orElse(null);
        if (job == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        job.setStatus(AutomationJobStatus.RUNNING);
        job.setWorkerId(request.workerId());
        job.setClaimedAt(now);
        job.setStartedAt(now);
        job.setExecutionLog(truncate(appendLog(job.getExecutionLog(), "worker 已领取任务: " + request.workerId()), MAX_LOG_LENGTH));
        syncReminderOnClaim(job, request.workerId(), now);

        return new WorkerAutomationJobView(
            job.getJobNo(),
            job.getJobType().name(),
            job.getChannel(),
            job.getPayloadJson(),
            job.getPlannedAt(),
            job.getRetryCount()
        );
    }

    @Transactional
    public void completeJob(String jobNo, WorkerJobResultRequest request) {
        AutomationJob job = loadJob(jobNo);
        assertWorker(job, request.workerId());
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(AutomationJobStatus.SUCCESS);
        job.setFinishedAt(now);
        job.setWorkerId(request.workerId());
        job.setLastError(null);
        job.setResultJson(truncate(request.resultJson(), MAX_RESULT_JSON_LENGTH));
        job.setExecutionLog(truncate(mergeLogs(job.getExecutionLog(), request.executionLog(), "worker 回写成功结果"), MAX_LOG_LENGTH));
        syncReminderOnSuccess(job, request, now);
    }

    @Transactional
    public void failJob(String jobNo, WorkerJobResultRequest request) {
        AutomationJob job = loadJob(jobNo);
        assertWorker(job, request.workerId());
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(AutomationJobStatus.FAILED);
        job.setFinishedAt(now);
        job.setWorkerId(request.workerId());
        job.setLastError(truncate(request.errorMessage(), MAX_SHORT_TEXT_LENGTH));
        job.setExecutionLog(truncate(mergeLogs(job.getExecutionLog(), request.executionLog(), "worker 回写失败结果"), MAX_LOG_LENGTH));
        syncReminderOnFailure(job, request, now);
    }

    private AutomationJob loadJob(String jobNo) {
        return automationJobRepository.findByJobNo(jobNo)
            .orElseThrow(() -> new EntityNotFoundException("自动化任务不存在"));
    }

    private void assertWorker(AutomationJob job, String workerId) {
        if (job.getWorkerId() != null && !job.getWorkerId().equals(workerId)) {
            throw new IllegalArgumentException("任务已被其他 worker 领取");
        }
    }

    private void syncReminderOnClaim(AutomationJob job, String workerId, LocalDateTime now) {
        if (!"REMINDER_TASK".equals(job.getBizType())) {
            syncMessageTriggerExecutionOnClaim(job, workerId, now);
            return;
        }
        ReminderTask reminderTask = reminderTaskRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("提醒任务不存在"));
        reminderTask.setStatus(ReminderTaskStatus.READY);
        reminderTask.setStartedAt(now);
        reminderTask.setExecutionLog(
            truncate(appendLog(reminderTask.getExecutionLog(), "worker 已领取: " + workerId), MAX_LOG_LENGTH)
        );
    }

    private void syncReminderOnSuccess(AutomationJob job, WorkerJobResultRequest request, LocalDateTime now) {
        if (!"REMINDER_TASK".equals(job.getBizType())) {
            syncMessageTriggerExecutionOnSuccess(job, request, now);
            return;
        }
        ReminderTask reminderTask = reminderTaskRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("提醒任务不存在"));
        reminderTask.setStatus(ReminderTaskStatus.SENT);
        reminderTask.setSentAt(now);
        reminderTask.setFinishedAt(now);
        reminderTask.setFailReason(null);
        reminderTask.setCommandLine(truncate(request.commandLine(), MAX_COMMAND_LINE_LENGTH));
        reminderTask.setExecutionLog(
            truncate(mergeLogs(reminderTask.getExecutionLog(), request.executionLog(), "发送成功"), MAX_LOG_LENGTH)
        );
    }

    private void syncReminderOnFailure(AutomationJob job, WorkerJobResultRequest request, LocalDateTime now) {
        if (!"REMINDER_TASK".equals(job.getBizType())) {
            syncMessageTriggerExecutionOnFailure(job, request, now);
            return;
        }
        ReminderTask reminderTask = reminderTaskRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("提醒任务不存在"));
        reminderTask.setStatus(ReminderTaskStatus.FAILED);
        reminderTask.setFinishedAt(now);
        reminderTask.setFailReason(truncate(request.errorMessage(), MAX_SHORT_TEXT_LENGTH));
        reminderTask.setCommandLine(truncate(request.commandLine(), MAX_COMMAND_LINE_LENGTH));
        reminderTask.setExecutionLog(
            truncate(
                mergeLogs(reminderTask.getExecutionLog(), request.executionLog(), "发送失败: " + request.errorMessage()),
                MAX_LOG_LENGTH
            )
        );
    }

    private void syncMessageTriggerExecutionOnClaim(AutomationJob job, String workerId, LocalDateTime now) {
        if (!"MESSAGE_TRIGGER_EXECUTION".equals(job.getBizType())) {
            return;
        }
        MessageTriggerExecution execution = messageTriggerExecutionRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("消息规则执行记录不存在"));
        execution.setStatus("RUNNING");
        execution.setExecutionLog(truncate(appendLog(execution.getExecutionLog(), "worker 已领取: " + workerId), MAX_LOG_LENGTH));
    }

    private void syncMessageTriggerExecutionOnSuccess(AutomationJob job, WorkerJobResultRequest request, LocalDateTime now) {
        if (!"MESSAGE_TRIGGER_EXECUTION".equals(job.getBizType())) {
            return;
        }
        MessageTriggerExecution execution = messageTriggerExecutionRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("消息规则执行记录不存在"));
        execution.setStatus("SUCCESS");
        execution.setErrorMessage(null);
        execution.setExecutionLog(truncate(mergeLogs(execution.getExecutionLog(), request.executionLog(), "发送成功"), MAX_LOG_LENGTH));
    }

    private void syncMessageTriggerExecutionOnFailure(AutomationJob job, WorkerJobResultRequest request, LocalDateTime now) {
        if (!"MESSAGE_TRIGGER_EXECUTION".equals(job.getBizType())) {
            return;
        }
        MessageTriggerExecution execution = messageTriggerExecutionRepository.findById(job.getBizId())
            .orElseThrow(() -> new EntityNotFoundException("消息规则执行记录不存在"));
        execution.setStatus("FAILED");
        execution.setErrorMessage(truncate(request.errorMessage(), MAX_SHORT_TEXT_LENGTH));
        execution.setExecutionLog(
            truncate(
                mergeLogs(execution.getExecutionLog(), request.executionLog(), "发送失败: " + request.errorMessage()),
                MAX_LOG_LENGTH
            )
        );
    }

    private AutomationJobView toView(AutomationJob job) {
        return new AutomationJobView(
            job.getId(),
            job.getJobNo(),
            job.getJobType().name(),
            job.getBizType(),
            job.getBizId(),
            job.getChannel(),
            job.getStatus().name(),
            job.getPlannedAt(),
            job.getClaimedAt(),
            job.getStartedAt(),
            job.getFinishedAt(),
            job.getWorkerId(),
            job.getRetryCount(),
            job.getLastError(),
            job.getExecutionLog()
        );
    }

    private String appendLog(String original, String message) {
        String line = "[" + LocalDateTime.now() + "] " + message;
        if (original == null || original.isBlank()) {
            return line;
        }
        return original + System.lineSeparator() + line;
    }

    private String mergeLogs(String original, String externalLog, String summary) {
        String merged = appendLog(original, summary);
        if (externalLog == null || externalLog.isBlank()) {
            return merged;
        }
        return merged + System.lineSeparator() + externalLog;
    }

    private String writeJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("自动化任务载荷序列化失败", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
