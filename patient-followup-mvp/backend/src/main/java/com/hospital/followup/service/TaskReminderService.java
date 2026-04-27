package com.hospital.followup.service;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.ReminderTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.ReminderTaskStatus;
import com.hospital.followup.dto.admin.DailyBatchReminderRequest;
import com.hospital.followup.dto.admin.DailyBatchReminderResult;
import com.hospital.followup.dto.admin.ReminderTaskView;
import com.hospital.followup.dto.admin.TaskReminderSendRequest;
import com.hospital.followup.dto.admin.TaskReminderSendResult;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.ReminderTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskReminderService {

    private static final Logger log = LoggerFactory.getLogger(TaskReminderService.class);

    private static final Set<QuestionnaireTaskStatus> OPEN_TASK_STATUSES = Set.of(
        QuestionnaireTaskStatus.PENDING,
        QuestionnaireTaskStatus.OVERDUE
    );

    private static final Set<ReminderTaskStatus> ACTIVE_REMINDER_STATUSES = Set.of(
        ReminderTaskStatus.PENDING,
        ReminderTaskStatus.READY,
        ReminderTaskStatus.SENT
    );

    private final QuestionnaireTaskRepository taskRepository;
    private final ReminderTaskRepository reminderTaskRepository;
    private final RpaMessageService rpaMessageService;
    private final AutomationJobService automationJobService;
    private final ReminderQrCodeService reminderQrCodeService;

    public TaskReminderService(
        QuestionnaireTaskRepository taskRepository,
        ReminderTaskRepository reminderTaskRepository,
        RpaMessageService rpaMessageService,
        AutomationJobService automationJobService,
        ReminderQrCodeService reminderQrCodeService
    ) {
        this.taskRepository = taskRepository;
        this.reminderTaskRepository = reminderTaskRepository;
        this.rpaMessageService = rpaMessageService;
        this.automationJobService = automationJobService;
        this.reminderQrCodeService = reminderQrCodeService;
    }

    @Transactional
    public TaskReminderSendResult sendManualReminder(String taskNo, TaskReminderSendRequest request) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));

        int countdownSeconds = rpaMessageService.resolveCountdownSeconds(request.countdownSeconds());
        LocalDateTime plannedAt = LocalDateTime.now().plusSeconds(countdownSeconds);
        ReminderTask reminderTask = new ReminderTask();
        reminderTask.setQuestionnaireTask(task);
        reminderTask.setRuleCode("MANUAL_WECHAT_TEXT");
        reminderTask.setReminderChannel("WECHAT_RPA");
        reminderTask.setTargetConversation(request.targetConversation());
        reminderTask.setContentPreview(truncate(request.content(), 255));
        reminderTask.setPlannedAt(plannedAt);
        reminderTask.setExecutionLog(buildInitialLog(task, request, countdownSeconds, plannedAt));
        reminderTask.setStatus(ReminderTaskStatus.PENDING);
        reminderTaskRepository.save(reminderTask);

        String qrImagePath = reminderQrCodeService.ensurePlaceholderQr(task);
        automationJobService.createWechatReminderJob(
            reminderTask,
            task.getTaskNo(),
            request.targetConversation(),
            request.content(),
            countdownSeconds,
            plannedAt,
            qrImagePath
        );
        reminderTask.setExecutionLog(appendLog(
            reminderTask.getExecutionLog(),
            qrImagePath == null
                ? "已加入自动化任务队列（未附二维码），等待本地 worker 执行"
                : "已加入自动化任务队列（附占位二维码 " + qrImagePath + "），等待本地 worker 执行"
        ));
        return toSendResult(task, reminderTask, "提醒任务已加入队列", countdownSeconds, "QUEUED");
    }

    /**
     * 一键给今日（及之前）所有待填/逾期的任务创建微信提醒。
     * 默认优先按患者已绑定微信群发送，未绑定时回退到 patient.name，
     * 近窗口内（默认 2 小时）已经在队列 / 已发送的任务会被跳过，避免连点重发。
     */
    @Transactional
    public DailyBatchReminderResult sendDailyBatch(DailyBatchReminderRequest request) {
        LocalDate today = LocalDate.now();
        int countdownSeconds = rpaMessageService.resolveCountdownSeconds(request.resolvedCountdownSeconds());

        List<QuestionnaireTask> tasks = taskRepository
            .findByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(today, OPEN_TASK_STATUSES);

        Set<Long> recentlyRemindedTaskIds = new HashSet<>();
        if (request.resolvedSkipRecentlyReminded() && !tasks.isEmpty()) {
            LocalDateTime since = LocalDateTime.now().minusHours(request.resolvedRecentWindowHours());
            List<Long> taskIds = tasks.stream().map(QuestionnaireTask::getId).toList();
            List<ReminderTask> recent = reminderTaskRepository
                .findByQuestionnaireTaskIdInAndStatusInAndCreatedAtAfter(taskIds, ACTIVE_REMINDER_STATUSES, since);
            recent.forEach(r -> recentlyRemindedTaskIds.add(r.getQuestionnaireTask().getId()));
        }

        List<DailyBatchReminderResult.Item> items = new ArrayList<>(tasks.size());
        int queued = 0;
        int skipped = 0;
        int failed = 0;

        for (QuestionnaireTask task : tasks) {
            String taskNo = task.getTaskNo();
            String patientId = task.getPatient().getPatientId();
            Patient patient = task.getPatient();
            String patientName = safe(patient.getName());
            String stageName = task.getStage() == null ? null : task.getStage().getStageName();
            String dueDate = task.getDueDate() == null ? null : task.getDueDate().toString();
            String targetConversation = resolvePreferredConversation(patient);

            if (recentlyRemindedTaskIds.contains(task.getId())) {
                skipped++;
                items.add(new DailyBatchReminderResult.Item(
                    taskNo, patientId, patientName, stageName, dueDate, targetConversation,
                    "SKIPPED_RECENT_REMINDER",
                    request.resolvedRecentWindowHours() + " 小时内已有提醒，跳过"
                ));
                continue;
            }

            if (targetConversation.isEmpty()) {
                skipped++;
                items.add(new DailyBatchReminderResult.Item(
                    taskNo, patientId, patientName, stageName, dueDate, targetConversation,
                    "SKIPPED_NO_CONTACT",
                    "患者未绑定微信群且姓名为空，无法生成发送目标"
                ));
                continue;
            }

            String content = renderContent(request.contentTemplate(), task);
            if (content.isBlank()) {
                skipped++;
                items.add(new DailyBatchReminderResult.Item(
                    taskNo, patientId, patientName, stageName, dueDate, targetConversation,
                    "SKIPPED_NO_CONTACT",
                    "生成提醒内容失败"
                ));
                continue;
            }

            try {
                LocalDateTime plannedAt = LocalDateTime.now().plusSeconds(countdownSeconds);
                ReminderTask reminderTask = new ReminderTask();
                reminderTask.setQuestionnaireTask(task);
                reminderTask.setRuleCode("DAILY_BATCH_WECHAT_TEXT");
                reminderTask.setReminderChannel("WECHAT_RPA");
                reminderTask.setTargetConversation(targetConversation);
                reminderTask.setContentPreview(truncate(content, 255));
                reminderTask.setPlannedAt(plannedAt);
                reminderTask.setExecutionLog(buildBatchInitialLog(task, targetConversation, countdownSeconds, plannedAt));
                reminderTask.setStatus(ReminderTaskStatus.PENDING);
                reminderTaskRepository.save(reminderTask);

                String qrImagePath = reminderQrCodeService.ensurePlaceholderQr(task);
                automationJobService.createWechatReminderJob(
                    reminderTask,
                    task.getTaskNo(),
                    targetConversation,
                    content,
                    countdownSeconds,
                    plannedAt,
                    qrImagePath
                );
                reminderTask.setExecutionLog(appendLog(
                    reminderTask.getExecutionLog(),
                    qrImagePath == null ? "已加入自动化任务队列（未附二维码）" : "已加入自动化任务队列（附占位二维码）"
                ));

                queued++;
                items.add(new DailyBatchReminderResult.Item(
                    taskNo, patientId, patientName, stageName, dueDate, targetConversation,
                    "QUEUED",
                    qrImagePath == null
                        ? "已入队，倒计时 " + countdownSeconds + "s（仅文字）"
                        : "已入队，倒计时 " + countdownSeconds + "s（附占位二维码）"
                ));
            } catch (Exception error) {
                log.warn("[daily-batch] 任务 {} 入队失败: {}", taskNo, error.getMessage(), error);
                failed++;
                items.add(new DailyBatchReminderResult.Item(
                    taskNo, patientId, patientName, stageName, dueDate, targetConversation,
                    "FAILED",
                    error.getMessage() == null ? "未知错误" : error.getMessage()
                ));
            }
        }

        return new DailyBatchReminderResult(tasks.size(), queued, skipped, failed, items);
    }

    private String renderContent(String template, QuestionnaireTask task) {
        String patientName = safe(task.getPatient().getName());
        String stageName = task.getStage() == null ? "" : safe(task.getStage().getStageName());
        String dueDate = task.getDueDate() == null ? "" : task.getDueDate().toString();

        String content;
        if (template != null && !template.isBlank()) {
            content = template
                .replace("{patientName}", patientName)
                .replace("{stageName}", stageName)
                .replace("{dueDate}", dueDate);
        } else {
            content = patientName + "您好，您有一份" + (stageName.isEmpty() ? "" : stageName)
                + "随访问卷待填写。"
                + (dueDate.isEmpty() ? "" : "应填写日期：" + dueDate + "。")
                + "请您尽快完成填写，如已完成可忽略本消息。";
        }
        return content.trim();
    }

    private String buildBatchInitialLog(
        QuestionnaireTask task,
        String targetConversation,
        int countdownSeconds,
        LocalDateTime plannedAt
    ) {
        String logs = "";
        logs = appendLog(logs, "来源: 首页一键提醒");
        logs = appendLog(logs, "任务号: " + task.getTaskNo());
        logs = appendLog(logs, "患者: " + safe(task.getPatient().getName()) + " / " + task.getPatient().getPatientId());
        if (hasBoundChatroom(task.getPatient())) {
            logs = appendLog(logs, "优先使用患者已绑定微信群: " + safe(task.getPatient().getWechatGroupName()));
        }
        logs = appendLog(logs, "目标会话: " + targetConversation);
        logs = appendLog(logs, "发送前倒计时: " + countdownSeconds + " 秒");
        logs = appendLog(logs, "计划执行时间: " + plannedAt);
        return logs;
    }

    public String resolvePreferredConversation(Patient patient) {
        if (patient == null) {
            return "";
        }
        String groupName = safe(patient.getWechatGroupName());
        if (!groupName.isEmpty()) {
            return groupName;
        }
        String displayName = safe(patient.getWechatChatroomDisplayName());
        if (!displayName.isEmpty()) {
            return displayName;
        }
        return safe(patient.getName());
    }

    private boolean hasBoundChatroom(Patient patient) {
        return patient != null
            && (!safe(patient.getWechatGroupName()).isEmpty() || !safe(patient.getWechatChatroomDisplayName()).isEmpty());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Transactional(readOnly = true)
    public List<ReminderTaskView> listTaskReminders(String taskNo) {
        taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        return reminderTaskRepository.findByQuestionnaireTaskTaskNoOrderByCreatedAtDesc(taskNo)
            .stream()
            .map(this::toView)
            .toList();
    }

    private TaskReminderSendResult toSendResult(
        QuestionnaireTask task,
        ReminderTask reminderTask,
        String message,
        int countdownSeconds,
        String status
    ) {
        return new TaskReminderSendResult(
            reminderTask.getId(),
            task.getTaskNo(),
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            reminderTask.getTargetConversation(),
            reminderTask.getReminderChannel(),
            status,
            message,
            countdownSeconds,
            reminderTask.getStartedAt(),
            reminderTask.getSentAt(),
            reminderTask.getFinishedAt(),
            reminderTask.getExecutionLog(),
            reminderTask.getCommandLine()
        );
    }

    private ReminderTaskView toView(ReminderTask reminderTask) {
        return new ReminderTaskView(
            reminderTask.getId(),
            reminderTask.getQuestionnaireTask().getTaskNo(),
            reminderTask.getTargetConversation(),
            reminderTask.getContentPreview(),
            reminderTask.getReminderChannel(),
            reminderTask.getStatus().name(),
            reminderTask.getFailReason(),
            reminderTask.getPlannedAt(),
            reminderTask.getStartedAt(),
            reminderTask.getSentAt(),
            reminderTask.getFinishedAt(),
            reminderTask.getCommandLine(),
            reminderTask.getExecutionLog()
        );
    }

    private String buildInitialLog(
        QuestionnaireTask task,
        TaskReminderSendRequest request,
        int countdownSeconds,
        LocalDateTime plannedAt
    ) {
        String log = "";
        log = appendLog(log, "准备发送提醒");
        log = appendLog(log, "任务号: " + task.getTaskNo());
        log = appendLog(log, "患者: " + task.getPatient().getName() + " / " + task.getPatient().getPatientId());
        log = appendLog(log, "目标会话: " + request.targetConversation());
        log = appendLog(log, "发送前倒计时: " + countdownSeconds + " 秒");
        log = appendLog(log, "计划执行时间: " + plannedAt);
        return appendLog(log, "说明: 点击网页发送后，请在倒计时结束前切回微信窗口");
    }

    private String appendLog(String original, String message) {
        String line = "[" + LocalDateTime.now() + "] " + message;
        if (original == null || original.isBlank()) {
            return line;
        }
        return original + System.lineSeparator() + line;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
