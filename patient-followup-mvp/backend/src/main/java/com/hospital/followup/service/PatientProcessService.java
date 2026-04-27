package com.hospital.followup.service;

import com.hospital.followup.domain.AutomationJob;
import com.hospital.followup.domain.MessageTriggerExecution;
import com.hospital.followup.domain.MessageTriggerRule;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.PatientChatMessage;
import com.hospital.followup.domain.PatientProcessInstance;
import com.hospital.followup.domain.PatientProcessStepInstance;
import com.hospital.followup.domain.ProcessTemplate;
import com.hospital.followup.domain.ProcessTemplateStep;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.AutomationJobStatus;
import com.hospital.followup.domain.enums.PatientStatus;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.dto.admin.PatientProcessDashboardView;
import com.hospital.followup.dto.admin.PatientProcessDetailView;
import com.hospital.followup.dto.admin.PatientProcessExceptionCenterView;
import com.hospital.followup.dto.admin.PatientProcessExceptionItemView;
import com.hospital.followup.dto.admin.PatientProcessOverviewView;
import com.hospital.followup.dto.admin.PatientProcessStepView;
import com.hospital.followup.dto.admin.PatientProcessTemplateStepRequest;
import com.hospital.followup.dto.admin.PatientProcessTemplateStepView;
import com.hospital.followup.dto.admin.PatientProcessTemplateUpsertRequest;
import com.hospital.followup.dto.admin.PatientProcessTemplateView;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.repository.AutomationJobRepository;
import com.hospital.followup.repository.MessageTriggerExecutionRepository;
import com.hospital.followup.repository.MessageTriggerRuleRepository;
import com.hospital.followup.repository.PatientChatMessageRepository;
import com.hospital.followup.repository.PatientProcessInstanceRepository;
import com.hospital.followup.repository.PatientProcessStepInstanceRepository;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.ProcessTemplateRepository;
import com.hospital.followup.repository.ProcessTemplateStepRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PatientProcessService {

    private static final String DEFAULT_TEMPLATE_CODE = "PERIOP_STANDARD";

    private static final String INSTANCE_STATUS_ACTIVE = "ACTIVE";
    private static final String INSTANCE_STATUS_COMPLETED = "COMPLETED";
    private static final String INSTANCE_STATUS_WARNING = "WARNING";

    private static final String STEP_STATUS_UPCOMING = "UPCOMING";
    private static final String STEP_STATUS_READY = "READY";
    private static final String STEP_STATUS_RUNNING = "RUNNING";
    private static final String STEP_STATUS_WAITING_FEEDBACK = "WAITING_FEEDBACK";
    private static final String STEP_STATUS_COMPLETED = "COMPLETED";
    private static final String STEP_STATUS_WARNING = "WARNING";
    private static final String STEP_STATUS_SKIPPED = "SKIPPED";
    private static final String EXCEPTION_STEP_WARNING = "STEP_WARNING";
    private static final String EXCEPTION_FEEDBACK_TIMEOUT = "FEEDBACK_TIMEOUT";
    private static final String EXCEPTION_SEND_FAILURE = "SEND_FAILURE";
    private static final long FEEDBACK_TIMEOUT_HOURS = 24L;

    private static final String STEP_TYPE_MESSAGE = "MESSAGE";
    private static final String STEP_TYPE_QUESTIONNAIRE = "QUESTIONNAIRE";

    private static final String TRIGGER_MODE_BIND_GROUP = "EVENT_BIND_GROUP";
    private static final String TRIGGER_MODE_SURGERY_RELATIVE = "SURGERY_RELATIVE";

    private static final String RELATIVE_BASE_BIND_GROUP = "BIND_GROUP";
    private static final String RELATIVE_BASE_SURGERY_DATE = "SURGERY_DATE";

    private final ProcessTemplateRepository processTemplateRepository;
    private final ProcessTemplateStepRepository processTemplateStepRepository;
    private final PatientProcessInstanceRepository patientProcessInstanceRepository;
    private final PatientProcessStepInstanceRepository patientProcessStepInstanceRepository;
    private final PatientRepository patientRepository;
    private final QuestionnaireTaskRepository questionnaireTaskRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final MessageTriggerExecutionRepository messageTriggerExecutionRepository;
    private final MessageTriggerRuleRepository messageTriggerRuleRepository;
    private final AutomationJobRepository automationJobRepository;
    private final PatientChatMessageRepository patientChatMessageRepository;
    private final PatientProcessNumberGenerator patientProcessNumberGenerator;

    public PatientProcessService(
        ProcessTemplateRepository processTemplateRepository,
        ProcessTemplateStepRepository processTemplateStepRepository,
        PatientProcessInstanceRepository patientProcessInstanceRepository,
        PatientProcessStepInstanceRepository patientProcessStepInstanceRepository,
        PatientRepository patientRepository,
        QuestionnaireTaskRepository questionnaireTaskRepository,
        QuestionnaireResponseRepository questionnaireResponseRepository,
        MessageTriggerExecutionRepository messageTriggerExecutionRepository,
        MessageTriggerRuleRepository messageTriggerRuleRepository,
        AutomationJobRepository automationJobRepository,
        PatientChatMessageRepository patientChatMessageRepository,
        PatientProcessNumberGenerator patientProcessNumberGenerator
    ) {
        this.processTemplateRepository = processTemplateRepository;
        this.processTemplateStepRepository = processTemplateStepRepository;
        this.patientProcessInstanceRepository = patientProcessInstanceRepository;
        this.patientProcessStepInstanceRepository = patientProcessStepInstanceRepository;
        this.patientRepository = patientRepository;
        this.questionnaireTaskRepository = questionnaireTaskRepository;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
        this.messageTriggerExecutionRepository = messageTriggerExecutionRepository;
        this.messageTriggerRuleRepository = messageTriggerRuleRepository;
        this.automationJobRepository = automationJobRepository;
        this.patientChatMessageRepository = patientChatMessageRepository;
        this.patientProcessNumberGenerator = patientProcessNumberGenerator;
    }

    @Transactional
    public PatientProcessDashboardView getDashboard(String keyword) {
        ensureBuiltInTemplate();
        List<Patient> patients = loadPatients(keyword);
        List<PatientProcessOverviewView> items = new ArrayList<>();
        int waitingFeedbackPatients = 0;
        int warningPatients = 0;
        for (Patient patient : patients) {
            PatientProcessInstance instance = syncProcessForPatient(patient);
            PatientProcessOverviewView overview = toOverviewView(instance);
            items.add(overview);
            if (instance.getWaitingFeedbackCount() > 0) {
                waitingFeedbackPatients++;
            }
            if (instance.getWarningStepCount() > 0) {
                warningPatients++;
            }
        }
        return new PatientProcessDashboardView(
            patients.size(),
            items.size(),
            waitingFeedbackPatients,
            warningPatients,
            items.stream().sorted(Comparator.comparing(PatientProcessOverviewView::updatedAt).reversed()).toList()
        );
    }

    @Transactional
    public PatientProcessDetailView getPatientProcessDetail(String patientId) {
        ensureBuiltInTemplate();
        Patient patient = loadPatient(patientId);
        PatientProcessInstance instance = syncProcessForPatient(patient);
        List<PatientProcessStepView> steps = patientProcessStepInstanceRepository.findByInstance_IdOrderBySortOrderAsc(instance.getId())
            .stream()
            .map(this::toStepView)
            .toList();
        return new PatientProcessDetailView(
            instance.getInstanceNo(),
            toPatientView(patient),
            instance.getTemplate().getTemplateCode(),
            instance.getTemplate().getTemplateName(),
            instance.getStatus(),
            instance.getCurrentStepCode(),
            instance.getCurrentStepName(),
            instance.getTotalStepCount(),
            instance.getCompletedStepCount(),
            instance.getWaitingFeedbackCount(),
            instance.getWarningStepCount(),
            calcProgress(instance.getCompletedStepCount(), instance.getTotalStepCount()),
            instance.getSummaryText(),
            instance.getStartedAt(),
            instance.getFinishedAt(),
            steps
        );
    }

    @Transactional
    public List<PatientProcessTemplateView> listTemplates() {
        ensureBuiltInTemplate();
        return processTemplateRepository.findAllByOrderByDefaultTemplateDescActiveDescUpdatedAtDesc()
            .stream()
            .map(this::toTemplateView)
            .toList();
    }

    @Transactional
    public PatientProcessTemplateView createTemplate(PatientProcessTemplateUpsertRequest request) {
        validateTemplateRequest(request);
        ProcessTemplate template = new ProcessTemplate();
        applyTemplateChanges(template, request, true);
        processTemplateRepository.save(template);
        replaceTemplateSteps(template, request.steps());
        return toTemplateView(template);
    }

    @Transactional
    public PatientProcessTemplateView updateTemplate(Long id, PatientProcessTemplateUpsertRequest request) {
        validateTemplateRequest(request);
        ProcessTemplate template = processTemplateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("流程模板不存在"));
        applyTemplateChanges(template, request, false);
        replaceTemplateSteps(template, request.steps());
        return toTemplateView(template);
    }

    @Transactional
    public PatientProcessDetailView syncAndGetPatientProcessDetail(String patientId) {
        return getPatientProcessDetail(patientId);
    }

    @Transactional
    public PatientProcessExceptionCenterView getExceptionCenter() {
        ensureBuiltInTemplate();
        List<Patient> patients = loadPatients(null);
        List<PatientProcessExceptionItemView> items = new ArrayList<>();
        int warningCount = 0;
        int feedbackTimeoutCount = 0;
        int sendFailureCount = 0;
        for (Patient patient : patients) {
            PatientProcessInstance instance = syncProcessForPatient(patient);
            for (PatientProcessStepInstance step : patientProcessStepInstanceRepository.findByInstance_IdOrderBySortOrderAsc(instance.getId())) {
                PatientProcessExceptionItemView item = toExceptionItem(instance, step);
                if (item == null) {
                    continue;
                }
                items.add(item);
                if (EXCEPTION_FEEDBACK_TIMEOUT.equals(item.exceptionType())) {
                    feedbackTimeoutCount++;
                } else if (EXCEPTION_SEND_FAILURE.equals(item.exceptionType())) {
                    sendFailureCount++;
                } else {
                    warningCount++;
                }
            }
        }
        items.sort(Comparator.comparing(PatientProcessExceptionItemView::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        return new PatientProcessExceptionCenterView(items.size(), warningCount, feedbackTimeoutCount, sendFailureCount, items);
    }

    @Transactional
    public PatientProcessInstance syncProcessForPatient(Patient patient) {
        ProcessTemplate template = selectTemplate(patient);
        PatientProcessInstance instance = patientProcessInstanceRepository.findByPatient_Id(patient.getId())
            .orElseGet(() -> createInstance(patient, template));
        if (!Objects.equals(instance.getTemplate().getId(), template.getId())) {
            instance.setTemplate(template);
        }
        List<ProcessTemplateStep> templateSteps = processTemplateStepRepository.findByTemplate_IdOrderBySortOrderAsc(template.getId());
        Map<String, PatientProcessStepInstance> existingSteps = new HashMap<>();
        for (PatientProcessStepInstance item : patientProcessStepInstanceRepository.findByInstance_IdOrderBySortOrderAsc(instance.getId())) {
            existingSteps.put(item.getStepCode(), item);
        }
        Set<String> activeCodes = new HashSet<>();
        for (ProcessTemplateStep templateStep : templateSteps) {
            PatientProcessStepInstance stepInstance = existingSteps.get(templateStep.getStepCode());
            if (stepInstance == null) {
                stepInstance = new PatientProcessStepInstance();
                stepInstance.setInstance(instance);
                stepInstance.setTemplateStep(templateStep);
                stepInstance.setStepCode(templateStep.getStepCode());
            }
            syncStepBasics(stepInstance, templateStep, patient);
            activeCodes.add(templateStep.getStepCode());
            patientProcessStepInstanceRepository.save(stepInstance);
        }
        for (PatientProcessStepInstance stale : existingSteps.values()) {
            if (!activeCodes.contains(stale.getStepCode())) {
                patientProcessStepInstanceRepository.delete(stale);
            }
        }
        refreshInstanceSummary(instance);
        return patientProcessInstanceRepository.save(instance);
    }

    private void syncStepBasics(PatientProcessStepInstance stepInstance, ProcessTemplateStep templateStep, Patient patient) {
        MessageTriggerRule linkedTask = findLinkedTaskDefinition(templateStep.getMessageRuleCode());
        stepInstance.setTemplateStep(templateStep);
        stepInstance.setStepCode(templateStep.getStepCode());
        stepInstance.setStepName(templateStep.getStepName());
        stepInstance.setSortOrder(templateStep.getSortOrder());
        stepInstance.setStepType(templateStep.getStepType());
        stepInstance.setTriggerMode(templateStep.getTriggerMode());
        stepInstance.setRelativeDayOffset(templateStep.getRelativeDayOffset());
        stepInstance.setRelativeBase(templateStep.getRelativeBase());
        stepInstance.setLinkedMessageRuleCode(templateStep.getMessageRuleCode());
        stepInstance.setFeedbackRequired(resolveFeedbackRequired(templateStep, linkedTask));
        stepInstance.setDisplayHint(templateStep.getDescription());
        if (!matchesSurgeryTag(templateStep, patient)) {
            stepInstance.setStatus(STEP_STATUS_SKIPPED);
            stepInstance.setStatusReason("不适用当前手术场次");
            stepInstance.setFeedbackSummary(null);
            stepInstance.setLinkedQuestionnaireTaskNo(null);
            stepInstance.setLinkedQuestionnaireStatus(null);
            stepInstance.setLinkedAutomationJobNo(null);
            stepInstance.setLinkedAutomationJobStatus(null);
            stepInstance.setTriggeredAt(null);
            stepInstance.setCompletedAt(null);
            stepInstance.setPlannedDate(null);
            stepInstance.setPlannedAt(null);
            return;
        }
        resolvePlanTime(stepInstance, patient);
        if (!Boolean.TRUE.equals(templateStep.getEnabled())) {
            stepInstance.setStatus(STEP_STATUS_SKIPPED);
            stepInstance.setStatusReason("模板中已禁用");
            stepInstance.setPlannedAt(null);
            return;
        }
        if (STEP_TYPE_QUESTIONNAIRE.equals(templateStep.getStepType())) {
            syncQuestionnaireStep(stepInstance, patient);
        } else {
            syncMessageStep(stepInstance, patient);
        }
    }

    private void syncQuestionnaireStep(PatientProcessStepInstance stepInstance, Patient patient) {
        QuestionnaireTask task = findMatchingQuestionnaireTask(stepInstance, patient);
        stepInstance.setLinkedAutomationJobNo(null);
        stepInstance.setLinkedAutomationJobStatus(null);
        stepInstance.setTriggeredAt(null);
        if (task == null) {
            if (stepInstance.getPlannedDate() == null) {
                stepInstance.setStatus(STEP_STATUS_UPCOMING);
                stepInstance.setStatusReason("等待患者补充手术日期");
            } else if (stepInstance.getPlannedDate().isAfter(LocalDate.now())) {
                stepInstance.setStatus(STEP_STATUS_UPCOMING);
                stepInstance.setStatusReason("尚未到计划日期");
            } else {
                stepInstance.setStatus(STEP_STATUS_WARNING);
                stepInstance.setStatusReason("未找到对应问卷任务");
            }
            stepInstance.setLinkedQuestionnaireTaskNo(null);
            stepInstance.setLinkedQuestionnaireStatus(null);
            stepInstance.setCompletedAt(null);
            stepInstance.setFeedbackSummary(null);
            return;
        }
        stepInstance.setLinkedQuestionnaireTaskNo(task.getTaskNo());
        stepInstance.setLinkedQuestionnaireStatus(task.getStatus().name());
        stepInstance.setTriggeredAt(startOfDay(task.getDueDate()));
        stepInstance.setCompletedAt(task.getFinishedAt());
        stepInstance.setFeedbackSummary(task.getStatus() == QuestionnaireTaskStatus.COMPLETED ? "患者已完成问卷" : null);
        if (task.getStatus() == QuestionnaireTaskStatus.COMPLETED) {
            stepInstance.setStatus(STEP_STATUS_COMPLETED);
            stepInstance.setStatusReason("问卷已填写");
        } else if (task.getStatus() == QuestionnaireTaskStatus.CANCELLED) {
            stepInstance.setStatus(STEP_STATUS_SKIPPED);
            stepInstance.setStatusReason("问卷任务已取消");
        } else if (task.getStatus() == QuestionnaireTaskStatus.OVERDUE) {
            stepInstance.setStatus(STEP_STATUS_WARNING);
            stepInstance.setStatusReason("问卷已逾期未填写");
        } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            stepInstance.setStatus(STEP_STATUS_WAITING_FEEDBACK);
            stepInstance.setStatusReason("问卷已发送，等待患者填写");
        } else if (task.getDueDate() != null && task.getDueDate().isEqual(LocalDate.now())) {
            stepInstance.setStatus(STEP_STATUS_READY);
            stepInstance.setStatusReason("今天应发送/填写问卷");
        } else {
            stepInstance.setStatus(STEP_STATUS_UPCOMING);
            stepInstance.setStatusReason("等待计划日期");
        }
    }

    private void syncMessageStep(PatientProcessStepInstance stepInstance, Patient patient) {
        MessageTriggerExecution execution = findMatchingExecution(stepInstance, patient);
        stepInstance.setLinkedQuestionnaireTaskNo(null);
        stepInstance.setLinkedQuestionnaireStatus(null);
        if (execution == null) {
            stepInstance.setLinkedAutomationJobNo(null);
            stepInstance.setLinkedAutomationJobStatus(null);
            stepInstance.setTriggeredAt(null);
            stepInstance.setCompletedAt(null);
            if (RELATIVE_BASE_BIND_GROUP.equals(stepInstance.getRelativeBase())
                && !StringUtils.hasText(patient.getWechatChatroomUsername())) {
                stepInstance.setStatus(STEP_STATUS_UPCOMING);
                stepInstance.setStatusReason("等待绑定微信群");
            } else if (stepInstance.getPlannedDate() == null) {
                stepInstance.setStatus(STEP_STATUS_UPCOMING);
                stepInstance.setStatusReason("等待计划日期");
            } else if (stepInstance.getPlannedDate().isAfter(LocalDate.now())) {
                stepInstance.setStatus(STEP_STATUS_UPCOMING);
                stepInstance.setStatusReason("尚未到发送日期");
            } else if (stepInstance.getPlannedDate().isEqual(LocalDate.now())) {
                stepInstance.setStatus(STEP_STATUS_READY);
                stepInstance.setStatusReason("今天应触发发送");
            } else {
                stepInstance.setStatus(STEP_STATUS_WARNING);
                stepInstance.setStatusReason("已过计划日期但未检测到发送记录");
            }
            if (stepInstance.getFeedbackRequired()) {
                stepInstance.setFeedbackSummary("发送后等待患者反馈");
            } else {
                stepInstance.setFeedbackSummary(null);
            }
            return;
        }
        stepInstance.setTriggeredAt(execution.getTriggeredAt());
        stepInstance.setLinkedAutomationJobNo(execution.getAutomationJobNo());
        stepInstance.setLinkedAutomationJobStatus(resolveAutomationJobStatus(execution.getAutomationJobNo()));
        if ("SUCCESS".equalsIgnoreCase(execution.getStatus())) {
            if (stepInstance.getFeedbackRequired()) {
                MessageTriggerRule linkedTask = findLinkedTaskDefinition(stepInstance.getLinkedMessageRuleCode());
                PatientChatMessage feedback = findFeedbackAfter(stepInstance, linkedTask, patient, execution.getTriggeredAt());
                if (feedback != null) {
                    stepInstance.setStatus(STEP_STATUS_COMPLETED);
                    stepInstance.setCompletedAt(feedback.getMessageTime() == null ? feedback.getCreatedAt() : feedback.getMessageTime());
                    stepInstance.setFeedbackSummary("患者已反馈: " + safe(feedback.getContentPreview()));
                    stepInstance.setStatusReason(resolveFeedbackSuccessReason(linkedTask));
                } else {
                    stepInstance.setStatus(STEP_STATUS_WAITING_FEEDBACK);
                    stepInstance.setCompletedAt(null);
                    stepInstance.setFeedbackSummary(resolveFeedbackWaitingSummary(linkedTask));
                    stepInstance.setStatusReason(resolveFeedbackWaitingReason(linkedTask));
                }
            } else {
                stepInstance.setStatus(STEP_STATUS_COMPLETED);
                stepInstance.setCompletedAt(execution.getTriggeredAt());
                stepInstance.setFeedbackSummary("消息已发送");
                stepInstance.setStatusReason("消息发送成功");
            }
        } else if ("FAILED".equalsIgnoreCase(execution.getStatus())) {
            stepInstance.setStatus(STEP_STATUS_WARNING);
            stepInstance.setCompletedAt(null);
            stepInstance.setFeedbackSummary(null);
            stepInstance.setStatusReason("发送失败: " + safe(execution.getErrorMessage()));
        } else if ("RUNNING".equalsIgnoreCase(execution.getStatus())) {
            stepInstance.setStatus(STEP_STATUS_RUNNING);
            stepInstance.setCompletedAt(null);
            stepInstance.setFeedbackSummary("worker 正在执行");
            stepInstance.setStatusReason("自动化任务执行中");
        } else {
            stepInstance.setStatus(STEP_STATUS_READY);
            stepInstance.setCompletedAt(null);
            stepInstance.setFeedbackSummary("已入队等待发送");
            stepInstance.setStatusReason("自动化任务已排队");
        }
    }

    private void refreshInstanceSummary(PatientProcessInstance instance) {
        List<PatientProcessStepInstance> steps = patientProcessStepInstanceRepository.findByInstance_IdOrderBySortOrderAsc(instance.getId());
        int total = steps.size();
        int completed = 0;
        int waitingFeedback = 0;
        int warning = 0;
        PatientProcessStepInstance current = null;
        for (PatientProcessStepInstance step : steps) {
            if (STEP_STATUS_COMPLETED.equals(step.getStatus()) || STEP_STATUS_SKIPPED.equals(step.getStatus())) {
                completed++;
            }
            if (STEP_STATUS_WAITING_FEEDBACK.equals(step.getStatus())) {
                waitingFeedback++;
            }
            if (STEP_STATUS_WARNING.equals(step.getStatus())) {
                warning++;
            }
            if (current == null && !STEP_STATUS_COMPLETED.equals(step.getStatus()) && !STEP_STATUS_SKIPPED.equals(step.getStatus())) {
                current = step;
            }
        }
        instance.setTotalStepCount(total);
        instance.setCompletedStepCount(completed);
        instance.setWaitingFeedbackCount(waitingFeedback);
        instance.setWarningStepCount(warning);
        instance.setStartedAt(instance.getCreatedAt());
        if (current == null) {
            instance.setStatus(INSTANCE_STATUS_COMPLETED);
            instance.setCurrentStepCode(null);
            instance.setCurrentStepName("流程已完成");
            instance.setFinishedAt(LocalDateTime.now());
            instance.setSummaryText("全部流程节点已完成");
        } else {
            instance.setCurrentStepCode(current.getStepCode());
            instance.setCurrentStepName(current.getStepName());
            instance.setFinishedAt(null);
            instance.setStatus(warning > 0 ? INSTANCE_STATUS_WARNING : INSTANCE_STATUS_ACTIVE);
            instance.setSummaryText(buildSummaryText(current, waitingFeedback, warning));
        }
    }

    private String buildSummaryText(PatientProcessStepInstance current, int waitingFeedback, int warning) {
        List<String> parts = new ArrayList<>();
        parts.add("当前节点：" + current.getStepName());
        if (StringUtils.hasText(current.getStatusReason())) {
            parts.add(current.getStatusReason());
        }
        if (waitingFeedback > 0) {
            parts.add("待反馈 " + waitingFeedback + " 步");
        }
        if (warning > 0) {
            parts.add("异常 " + warning + " 步");
        }
        return String.join(" · ", parts);
    }

    private QuestionnaireTask findMatchingQuestionnaireTask(PatientProcessStepInstance stepInstance, Patient patient) {
        List<QuestionnaireTask> tasks = questionnaireTaskRepository.findByPatient_Id(patient.getId());
        for (QuestionnaireTask task : tasks) {
            if (stepInstance.getPlannedDate() != null && stepInstance.getPlannedDate().equals(task.getDueDate())) {
                return task;
            }
            if (StringUtils.hasText(stepInstance.getTemplateStep().getStageCode())
                && stepInstance.getTemplateStep().getStageCode().equals(task.getStage().getStageCode())) {
                return task;
            }
            if (StringUtils.hasText(stepInstance.getTemplateStep().getTemplateCode())
                && stepInstance.getTemplateStep().getTemplateCode().equals(task.getTemplate().getTemplateCode())) {
                return task;
            }
        }
        return null;
    }

    private MessageTriggerExecution findMatchingExecution(PatientProcessStepInstance stepInstance, Patient patient) {
        List<MessageTriggerExecution> executions = messageTriggerExecutionRepository.findByPatient_Id(patient.getId());
        executions.sort(Comparator.comparing(MessageTriggerExecution::getTriggeredAt, Comparator.nullsLast(LocalDateTime::compareTo)));
        for (MessageTriggerExecution execution : executions) {
            if (StringUtils.hasText(stepInstance.getLinkedMessageRuleCode())
                && !stepInstance.getLinkedMessageRuleCode().equals(execution.getRule().getRuleCode())) {
                continue;
            }
            if (!StringUtils.hasText(stepInstance.getLinkedMessageRuleCode())) {
                if (RELATIVE_BASE_BIND_GROUP.equals(stepInstance.getRelativeBase())
                    && !MessageTriggerRuleService.TRIGGER_BIND_GROUP_IMMEDIATE.equals(execution.getTriggerType())) {
                    continue;
                }
                if (RELATIVE_BASE_SURGERY_DATE.equals(stepInstance.getRelativeBase())
                    && !MessageTriggerRuleService.TRIGGER_SURGERY_RELATIVE_DAY.equals(execution.getTriggerType())) {
                    continue;
                }
                if (stepInstance.getPlannedDate() != null && execution.getPlannedAt() != null
                    && !stepInstance.getPlannedDate().equals(execution.getPlannedAt().toLocalDate())) {
                    continue;
                }
            }
            return execution;
        }
        return null;
    }

    private boolean matchesSurgeryTag(ProcessTemplateStep templateStep, Patient patient) {
        String applicableTags = trimToNull(templateStep.getApplicableSurgeryTags());
        if (!StringUtils.hasText(applicableTags)) {
            return true;
        }
        String patientTag = normalizeSurgeryTag(patient.getSurgeryScheduleTag());
        if (!StringUtils.hasText(patientTag)) {
            return false;
        }
        return splitTags(applicableTags).contains(patientTag);
    }

    private List<String> splitTags(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split("[,，/\\n\\r]+"))
            .stream()
            .map(this::normalizeSurgeryTag)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }

    private String normalizeSurgeryTag(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }

    private PatientChatMessage findFirstFeedbackAfter(Patient patient, LocalDateTime triggeredAt) {
        if (triggeredAt == null) {
            return null;
        }
        return patientChatMessageRepository.findTop20ByPatientPatientIdOrderByMessageTimeDescCreatedAtDesc(resolvePatientIdentity(patient))
            .stream()
            .filter(message -> "INBOUND".equalsIgnoreCase(message.getDirection()))
            .filter(message -> {
                LocalDateTime time = message.getMessageTime() == null ? message.getCreatedAt() : message.getMessageTime();
                return !time.isBefore(triggeredAt);
            })
            .min(Comparator.comparing(message -> message.getMessageTime() == null ? message.getCreatedAt() : message.getMessageTime()))
            .orElse(null);
    }

    private String resolveAutomationJobStatus(String jobNo) {
        if (!StringUtils.hasText(jobNo)) {
            return null;
        }
        return automationJobRepository.findByJobNo(jobNo)
            .map(job -> job.getStatus().name())
            .orElse(null);
    }

    private void resolvePlanTime(PatientProcessStepInstance stepInstance, Patient patient) {
        if (RELATIVE_BASE_BIND_GROUP.equals(stepInstance.getRelativeBase())) {
            stepInstance.setPlannedDate(patient.getWechatChatroomUsername() == null ? null : patient.getUpdatedAt().toLocalDate());
            stepInstance.setPlannedAt(patient.getWechatChatroomUsername() == null ? null : patient.getUpdatedAt());
            return;
        }
        if (patient.getSurgeryDate() == null) {
            stepInstance.setPlannedDate(null);
            stepInstance.setPlannedAt(null);
            return;
        }
        LocalDate plannedDate = patient.getSurgeryDate().plusDays(stepInstance.getRelativeDayOffset() == null ? 0 : stepInstance.getRelativeDayOffset());
        stepInstance.setPlannedDate(plannedDate);
        stepInstance.setPlannedAt(startOfDay(plannedDate));
    }

    private PatientProcessOverviewView toOverviewView(PatientProcessInstance instance) {
        Patient patient = instance.getPatient();
        return new PatientProcessOverviewView(
            instance.getInstanceNo(),
            resolvePatientIdentity(patient),
            safe(patient.getName()),
            safe(patient.getDiagnosis()),
            patient.getSurgeryDate(),
            instance.getTemplate().getTemplateName(),
            instance.getStatus(),
            instance.getCurrentStepCode(),
            instance.getCurrentStepName(),
            instance.getTotalStepCount(),
            instance.getCompletedStepCount(),
            instance.getWaitingFeedbackCount(),
            instance.getWarningStepCount(),
            calcProgress(instance.getCompletedStepCount(), instance.getTotalStepCount()),
            instance.getSummaryText(),
            instance.getUpdatedAt()
        );
    }

    private PatientProcessStepView toStepView(PatientProcessStepInstance step) {
        QuestionnaireTask questionnaireTask = null;
        QuestionnaireResponse questionnaireResponse = null;
        if (StringUtils.hasText(step.getLinkedQuestionnaireTaskNo())) {
            questionnaireTask = questionnaireTaskRepository.findByTaskNo(step.getLinkedQuestionnaireTaskNo()).orElse(null);
            questionnaireResponse = questionnaireResponseRepository.findFirstByTaskTaskNoOrderBySubmittedAtDesc(step.getLinkedQuestionnaireTaskNo())
                .orElse(null);
        }
        AutomationJob automationJob = null;
        if (StringUtils.hasText(step.getLinkedAutomationJobNo())) {
            automationJob = automationJobRepository.findByJobNo(step.getLinkedAutomationJobNo()).orElse(null);
        }
        return new PatientProcessStepView(
            step.getId(),
            step.getStepCode(),
            step.getStepName(),
            step.getSortOrder(),
            step.getStepType(),
            step.getTriggerMode(),
            step.getRelativeDayOffset(),
            step.getRelativeBase(),
            step.getStatus(),
            step.getStatusReason(),
            step.getFeedbackSummary(),
            step.getFeedbackRequired(),
            step.getPlannedDate(),
            step.getPlannedAt(),
            step.getTriggeredAt(),
            step.getCompletedAt(),
            step.getLinkedQuestionnaireTaskNo(),
            step.getLinkedQuestionnaireStatus(),
            questionnaireTask == null ? null : questionnaireTask.getDueDate(),
            questionnaireTask == null ? null : questionnaireTask.getFinishedAt(),
            questionnaireResponse == null ? null : questionnaireResponse.getSubmittedAt(),
            questionnaireResponse == null ? null : summarizeQuestionnaireResponse(questionnaireResponse.getAnswersJson()),
            step.getLinkedAutomationJobNo(),
            step.getLinkedAutomationJobStatus(),
            automationJob == null ? null : automationJob.getLastError(),
            automationJob == null ? null : automationJob.getExecutionLog(),
            step.getLinkedMessageRuleCode(),
            step.getTemplateStep() == null ? null : step.getTemplateStep().getApplicableSurgeryTags(),
            step.getDisplayHint()
        );
    }

    private PatientProcessTemplateView toTemplateView(ProcessTemplate template) {
        List<PatientProcessTemplateStepView> steps = processTemplateStepRepository.findByTemplate_IdOrderBySortOrderAsc(template.getId())
            .stream()
            .map(step -> new PatientProcessTemplateStepView(
                step.getId(),
                step.getStepCode(),
                step.getStepName(),
                step.getSortOrder(),
                step.getStepType(),
                step.getTriggerMode(),
                step.getRelativeDayOffset(),
                step.getRelativeBase(),
                step.getDescription(),
                step.getMessageRuleCode(),
                step.getStageCode(),
                step.getTemplateCode(),
                step.getCompletionRule(),
                step.getApplicableSurgeryTags(),
                step.getFeedbackRequired(),
                step.getEnabled()
            ))
            .toList();
        return new PatientProcessTemplateView(
            template.getId(),
            template.getTemplateCode(),
            template.getTemplateName(),
            template.getTemplateCategory(),
            template.getDescription(),
            template.getActive(),
            template.getDefaultTemplate(),
            template.getBuiltIn(),
            steps.size(),
            template.getUpdatedAt(),
            steps
        );
    }

    private void validateTemplateRequest(PatientProcessTemplateUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.templateName())) {
            throw new IllegalArgumentException("流程模板名称不能为空");
        }
        if (request.steps() == null || request.steps().isEmpty()) {
            throw new IllegalArgumentException("请至少配置一个流程节点");
        }
        int order = 1;
        for (PatientProcessTemplateStepRequest step : request.steps()) {
            if (step == null || !StringUtils.hasText(step.stepName())) {
                throw new IllegalArgumentException("流程节点名称不能为空");
            }
            if (!STEP_TYPE_MESSAGE.equals(normalizeStepType(step.stepType()))
                && !STEP_TYPE_QUESTIONNAIRE.equals(normalizeStepType(step.stepType()))) {
                throw new IllegalArgumentException("流程节点类型不支持");
            }
            if (!TRIGGER_MODE_BIND_GROUP.equals(normalizeTriggerMode(step.triggerMode()))
                && !TRIGGER_MODE_SURGERY_RELATIVE.equals(normalizeTriggerMode(step.triggerMode()))) {
                throw new IllegalArgumentException("流程节点触发方式不支持");
            }
            if (step.sortOrder() != null && step.sortOrder() < 1) {
                throw new IllegalArgumentException("流程节点排序必须大于 0");
            }
            order++;
        }
    }

    private void applyTemplateChanges(ProcessTemplate template, PatientProcessTemplateUpsertRequest request, boolean creating) {
        if (creating) {
            String templateCode = StringUtils.hasText(request.templateCode())
                ? request.templateCode().trim()
                : "PROC_" + System.currentTimeMillis();
            template.setTemplateCode(templateCode);
            template.setBuiltIn(Boolean.FALSE);
        }
        template.setTemplateName(request.templateName().trim());
        template.setTemplateCategory(trimToNull(request.templateCategory()));
        template.setDescription(trimToNull(request.description()));
        template.setActive(request.active() == null ? Boolean.TRUE : request.active());
        template.setDefaultTemplate(request.defaultTemplate() == null ? Boolean.FALSE : request.defaultTemplate());
        if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
            clearOtherDefaultTemplates(template.getId());
        }
    }

    private void replaceTemplateSteps(ProcessTemplate template, List<PatientProcessTemplateStepRequest> steps) {
        List<ProcessTemplateStep> current = processTemplateStepRepository.findByTemplate_IdOrderBySortOrderAsc(template.getId());
        if (!current.isEmpty()) {
            processTemplateStepRepository.deleteAll(current);
            processTemplateStepRepository.flush();
        }
        int index = 1;
        for (PatientProcessTemplateStepRequest item : steps) {
            ProcessTemplateStep step = new ProcessTemplateStep();
            applyTemplateStepChanges(step, template, item, index);
            processTemplateStepRepository.save(step);
            index++;
        }
    }

    private void applyTemplateStepChanges(
        ProcessTemplateStep step,
        ProcessTemplate template,
        PatientProcessTemplateStepRequest item,
        int fallbackSortOrder
    ) {
        step.setTemplate(template);
        step.setStepCode(StringUtils.hasText(item.stepCode()) ? item.stepCode().trim() : template.getTemplateCode() + "_STEP_" + fallbackSortOrder);
        step.setStepName(item.stepName().trim());
        step.setSortOrder(item.sortOrder() == null ? fallbackSortOrder : item.sortOrder());
        step.setStepType(normalizeStepType(item.stepType()));
        step.setTriggerMode(normalizeTriggerMode(item.triggerMode()));
        step.setRelativeDayOffset(item.relativeDayOffset() == null ? 0 : item.relativeDayOffset());
        step.setRelativeBase(resolveRelativeBase(step.getTriggerMode()));
        step.setDescription(trimToNull(item.description()));
        step.setMessageRuleCode(trimToNull(item.messageRuleCode()));
        step.setStageCode(trimToNull(item.stageCode()));
        step.setTemplateCode(trimToNull(item.templateCode()));
        step.setCompletionRule(trimToNull(item.completionRule()));
        step.setApplicableSurgeryTags(trimToNull(item.applicableSurgeryTags()));
        step.setFeedbackRequired(item.feedbackRequired() == null ? Boolean.FALSE : item.feedbackRequired());
        step.setEnabled(item.enabled() == null ? Boolean.TRUE : item.enabled());
    }

    private void clearOtherDefaultTemplates(Long currentId) {
        for (ProcessTemplate item : processTemplateRepository.findAll()) {
            if (!Objects.equals(item.getId(), currentId) && Boolean.TRUE.equals(item.getDefaultTemplate())) {
                item.setDefaultTemplate(Boolean.FALSE);
            }
        }
    }

    private ProcessTemplate selectTemplate(Patient patient) {
        return processTemplateRepository.findFirstByDefaultTemplateTrueAndActiveTrueOrderByUpdatedAtDesc()
            .orElseGet(this::ensureBuiltInTemplate);
    }

    private ProcessTemplate ensureBuiltInTemplate() {
        Optional<ProcessTemplate> existing = processTemplateRepository.findByTemplateCode(DEFAULT_TEMPLATE_CODE);
        if (existing.isPresent()) {
            ProcessTemplate template = existing.get();
            normalizeBuiltInTemplate(template);
            List<ProcessTemplateStep> currentSteps = processTemplateStepRepository.findByTemplate_IdOrderBySortOrderAsc(template.getId());
            if (currentSteps.isEmpty()) {
                seedBuiltInSteps(template);
            } else if (needsBuiltInBranchUpgrade(currentSteps)) {
                upgradeBuiltInStepsSafely(template, currentSteps);
            }
            return template;
        }
        ProcessTemplate template = new ProcessTemplate();
        template.setTemplateCode(DEFAULT_TEMPLATE_CODE);
        template.setTemplateName("围手术期标准流程");
        template.setTemplateCategory("围手术期");
        template.setDescription("覆盖术前问卷、术前提醒、术后须知和术后多时间点随访");
        template.setActive(Boolean.TRUE);
        template.setDefaultTemplate(Boolean.TRUE);
        template.setBuiltIn(Boolean.TRUE);
        processTemplateRepository.save(template);
        seedBuiltInSteps(template);
        return template;
    }

    private boolean needsBuiltInBranchUpgrade(List<ProcessTemplateStep> currentSteps) {
        boolean hasSlotBranch = currentSteps.stream().anyMatch(step -> "PREOP_NOTICE_SLOT_1".equals(step.getStepCode()));
        boolean hasLegacyGeneric = currentSteps.stream().anyMatch(step -> "PREOP_NOTICE_1D".equals(step.getStepCode()));
        return hasLegacyGeneric && !hasSlotBranch;
    }

    private void normalizeBuiltInTemplate(ProcessTemplate template) {
        boolean changed = false;
        if (!Boolean.TRUE.equals(template.getActive())) {
            template.setActive(Boolean.TRUE);
            changed = true;
        }
        if (!Boolean.TRUE.equals(template.getDefaultTemplate())) {
            template.setDefaultTemplate(Boolean.TRUE);
            changed = true;
        }
        if (!Boolean.TRUE.equals(template.getBuiltIn())) {
            template.setBuiltIn(Boolean.TRUE);
            changed = true;
        }
        if (!Objects.equals("围手术期标准流程", template.getTemplateName())) {
            template.setTemplateName("围手术期标准流程");
            changed = true;
        }
        if (!Objects.equals("围手术期", template.getTemplateCategory())) {
            template.setTemplateCategory("围手术期");
            changed = true;
        }
        if (!Objects.equals("覆盖术前问卷、术前提醒、术后须知和术后多时间点随访", template.getDescription())) {
            template.setDescription("覆盖术前问卷、术前提醒、术后须知和术后多时间点随访");
            changed = true;
        }
        if (changed) {
            processTemplateRepository.save(template);
        }
    }

    private void upgradeBuiltInStepsSafely(ProcessTemplate template, List<ProcessTemplateStep> currentSteps) {
        Map<String, ProcessTemplateStep> currentByCode = new HashMap<>();
        for (ProcessTemplateStep step : currentSteps) {
            currentByCode.put(step.getStepCode(), step);
        }
        ProcessTemplateStep legacyNoticeStep = currentByCode.get("PREOP_NOTICE_1D");
        for (PatientProcessTemplateStepRequest request : builtInTemplateSteps()) {
            ProcessTemplateStep target = currentByCode.get(request.stepCode());
            if (target == null && "PREOP_NOTICE_SLOT_1".equals(request.stepCode()) && legacyNoticeStep != null) {
                target = legacyNoticeStep;
            }
            if (target == null) {
                target = new ProcessTemplateStep();
                target.setTemplate(template);
            }
            applyTemplateStepChanges(target, template, request, request.sortOrder());
            processTemplateStepRepository.save(target);
        }
    }

    private void seedBuiltInSteps(ProcessTemplate template) {
        replaceTemplateSteps(template, builtInTemplateSteps());
    }

    private List<PatientProcessTemplateStepRequest> builtInTemplateSteps() {
        return List.of(
            new PatientProcessTemplateStepRequest("BIND_WELCOME", "入群欢迎消息", 10, STEP_TYPE_MESSAGE, TRIGGER_MODE_BIND_GROUP, 0, null, "患者绑定群后发送欢迎话术", null, null, null, null, null, false, true),
            new PatientProcessTemplateStepRequest("PREOP_Q_3D", "术前问卷", 20, STEP_TYPE_QUESTIONNAIRE, TRIGGER_MODE_SURGERY_RELATIVE, -3, null, "术前若干天发问卷", null, null, null, "QUESTIONNAIRE_COMPLETED", null, true, true),
            new PatientProcessTemplateStepRequest("PREOP_FLOW_1D", "术前一天院前检查流程", 30, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "术前一天发送院前检查就诊流程", null, null, null, null, null, false, true),
            new PatientProcessTemplateStepRequest("PREOP_CONFIRM_1D", "术前检查确认单", 40, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "术前一天发送术前检查确认单", null, null, null, null, null, false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_1", "术前注意事项·第1台", 50, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第1台手术患者专用注意事项", null, null, null, null, "第1台,1台,SLOT1", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_2", "术前注意事项·第2台", 51, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第2台手术患者专用注意事项", null, null, null, null, "第2台,2台,SLOT2", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_3", "术前注意事项·第3台", 52, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第3台手术患者专用注意事项", null, null, null, null, "第3台,3台,SLOT3", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_4", "术前注意事项·第4台", 53, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第4台手术患者专用注意事项", null, null, null, null, "第4台,4台,SLOT4", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_5", "术前注意事项·第5台", 54, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第5台手术患者专用注意事项", null, null, null, null, "第5台,5台,SLOT5", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_6", "术前注意事项·第6台", 55, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第6台手术患者专用注意事项", null, null, null, null, "第6台,6台,SLOT6", false, true),
            new PatientProcessTemplateStepRequest("PREOP_NOTICE_SLOT_7", "术前注意事项·第7台", 56, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, -1, null, "第二天第7台手术患者专用注意事项", null, null, null, null, "第7台,7台,SLOT7", false, true),
            new PatientProcessTemplateStepRequest("POSTOP_NOTICE_0D", "关节置换术后须知", 60, STEP_TYPE_MESSAGE, TRIGGER_MODE_SURGERY_RELATIVE, 0, null, "术后当天发送术后须知", null, null, null, null, null, false, true),
            new PatientProcessTemplateStepRequest("POSTOP_Q_0D", "手术当日问卷", 70, STEP_TYPE_QUESTIONNAIRE, TRIGGER_MODE_SURGERY_RELATIVE, 0, null, "术后当天问卷", null, null, null, "QUESTIONNAIRE_COMPLETED", null, true, true),
            new PatientProcessTemplateStepRequest("POSTOP_Q_7D", "术后七天问卷", 80, STEP_TYPE_QUESTIONNAIRE, TRIGGER_MODE_SURGERY_RELATIVE, 7, null, "术后第 7 天问卷", null, null, null, "QUESTIONNAIRE_COMPLETED", null, true, true),
            new PatientProcessTemplateStepRequest("POSTOP_Q_30D", "术后一个月问卷", 90, STEP_TYPE_QUESTIONNAIRE, TRIGGER_MODE_SURGERY_RELATIVE, 30, null, "术后第 30 天问卷", null, null, null, "QUESTIONNAIRE_COMPLETED", null, true, true),
            new PatientProcessTemplateStepRequest("POSTOP_Q_90D", "术后三个月问卷", 100, STEP_TYPE_QUESTIONNAIRE, TRIGGER_MODE_SURGERY_RELATIVE, 90, null, "术后第 90 天问卷", null, null, null, "QUESTIONNAIRE_COMPLETED", null, true, true)
        );
    }

    private PatientProcessInstance createInstance(Patient patient, ProcessTemplate template) {
        PatientProcessInstance instance = new PatientProcessInstance();
        instance.setPatient(patient);
        instance.setTemplate(template);
        instance.setInstanceNo(patientProcessNumberGenerator.nextNo());
        instance.setStatus(INSTANCE_STATUS_ACTIVE);
        instance.setStartedAt(LocalDateTime.now());
        return patientProcessInstanceRepository.save(instance);
    }

    private List<Patient> loadPatients(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return patientRepository.findByNameContainingIgnoreCaseOrPatientIdContainingIgnoreCaseOrderByCreatedAtDesc(keyword, keyword)
                .stream()
                .filter(this::shouldIncludeInProcessTracking)
                .sorted(Comparator.comparing(this::resolvePatientSortTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        }
        return patientRepository.findAll().stream()
            .filter(this::shouldIncludeInProcessTracking)
            .sorted(Comparator.comparing(this::resolvePatientSortTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
            .toList();
    }

    private boolean shouldIncludeInProcessTracking(Patient patient) {
        return patient.getStatus() != PatientStatus.ARCHIVED;
    }

    private LocalDateTime resolvePatientSortTime(Patient patient) {
        return patient.getUpdatedAt() != null ? patient.getUpdatedAt() : patient.getCreatedAt();
    }

    private Patient loadPatient(String patientId) {
        return patientRepository.findByPatientId(patientId)
            .or(() -> patientRepository.findByPatientNo(patientId))
            .orElseThrow(() -> new EntityNotFoundException("患者不存在"));
    }

    private String normalizeStepType(String stepType) {
        String value = trimToNull(stepType);
        if (value == null) {
            return STEP_TYPE_MESSAGE;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String normalizeTriggerMode(String triggerMode) {
        String value = trimToNull(triggerMode);
        if (value == null) {
            return TRIGGER_MODE_SURGERY_RELATIVE;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String resolveRelativeBase(String triggerMode) {
        return TRIGGER_MODE_BIND_GROUP.equals(triggerMode) ? RELATIVE_BASE_BIND_GROUP : RELATIVE_BASE_SURGERY_DATE;
    }

    private static LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private int calcProgress(Integer completed, Integer total) {
        if (total == null || total == 0) {
            return 0;
        }
        return Math.min(100, Math.max(0, completed == null ? 0 : (completed * 100 / total)));
    }

    private String resolvePatientIdentity(Patient patient) {
        if (StringUtils.hasText(patient.getPatientId())) {
            return patient.getPatientId();
        }
        return safe(patient.getPatientNo());
    }

    private PatientView toPatientView(Patient patient) {
        return new PatientView(
            resolvePatientIdentity(patient),
            patient.getName(),
            patient.getGender(),
            patient.getPhone(),
            patient.getBirthDate(),
            patient.getSurgeryDate(),
            patient.getSurgeryScheduleTag(),
            patient.getSurgeryTimeText(),
            patient.getDiagnosis(),
            patient.getSourceChannel(),
            patient.getWechatChatroomUsername(),
            patient.getWechatChatroomDisplayName(),
            patient.getWechatGroupName(),
            patient.getStatus() == null ? null : patient.getStatus().name(),
            patient.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String summarizeQuestionnaireResponse(String answersJson) {
        if (!StringUtils.hasText(answersJson)) {
            return null;
        }
        String normalized = answersJson.replaceAll("\\s+", " ").trim();
        return normalized.length() > 160 ? normalized.substring(0, 160) + "..." : normalized;
    }

    private PatientProcessExceptionItemView toExceptionItem(PatientProcessInstance instance, PatientProcessStepInstance step) {
        String exceptionType = null;
        String severity = "warning";
        if (STEP_STATUS_WARNING.equals(step.getStatus())) {
            exceptionType = StringUtils.hasText(step.getLinkedAutomationJobNo()) ? EXCEPTION_SEND_FAILURE : EXCEPTION_STEP_WARNING;
            if (EXCEPTION_SEND_FAILURE.equals(exceptionType)) {
                severity = "danger";
            }
        } else if (STEP_STATUS_WAITING_FEEDBACK.equals(step.getStatus())
            && step.getTriggeredAt() != null
            && step.getTriggeredAt().isBefore(LocalDateTime.now().minusHours(resolveFeedbackTimeoutHours(step)))) {
            exceptionType = EXCEPTION_FEEDBACK_TIMEOUT;
            severity = "warning";
        }
        if (exceptionType == null) {
            return null;
        }
        Patient patient = instance.getPatient();
        return new PatientProcessExceptionItemView(
            resolvePatientIdentity(patient),
            patient.getName(),
            instance.getTemplate().getTemplateName(),
            instance.getInstanceNo(),
            step.getStepCode(),
            step.getStepName(),
            exceptionType,
            severity,
            step.getStatus(),
            step.getStatusReason(),
            patient.getSurgeryDate(),
            patient.getSurgeryScheduleTag(),
            step.getPlannedDate(),
            step.getTriggeredAt(),
            step.getLinkedQuestionnaireTaskNo(),
            step.getLinkedAutomationJobNo(),
            step.getUpdatedAt()
        );
    }

    private MessageTriggerRule findLinkedTaskDefinition(String ruleCode) {
        String normalized = trimToNull(ruleCode);
        if (normalized == null) {
            return null;
        }
        return messageTriggerRuleRepository.findByRuleCode(normalized).orElse(null);
    }

    private boolean resolveFeedbackRequired(ProcessTemplateStep templateStep, MessageTriggerRule linkedTask) {
        if (linkedTask != null && Boolean.TRUE.equals(linkedTask.getFeedbackRequired())) {
            return true;
        }
        return Boolean.TRUE.equals(templateStep.getFeedbackRequired());
    }

    private long resolveFeedbackTimeoutHours(PatientProcessStepInstance step) {
        MessageTriggerRule linkedTask = findLinkedTaskDefinition(step.getLinkedMessageRuleCode());
        if (linkedTask != null && Boolean.TRUE.equals(linkedTask.getFeedbackRequired()) && linkedTask.getFeedbackTimeoutHours() != null) {
            return linkedTask.getFeedbackTimeoutHours();
        }
        return FEEDBACK_TIMEOUT_HOURS;
    }

    private PatientChatMessage findFeedbackAfter(
        PatientProcessStepInstance stepInstance,
        MessageTriggerRule linkedTask,
        Patient patient,
        LocalDateTime triggeredAt
    ) {
        if (linkedTask == null) {
            return findFirstFeedbackAfter(patient, triggeredAt);
        }
        String feedbackRule = linkedTask.getFeedbackRule();
        if (!StringUtils.hasText(feedbackRule) || MessageTriggerRuleService.FEEDBACK_RULE_ANY_MESSAGE.equalsIgnoreCase(feedbackRule)) {
            return findFirstFeedbackAfter(patient, triggeredAt);
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_KEYWORD.equalsIgnoreCase(feedbackRule)) {
            return findFirstKeywordFeedbackAfter(patient, triggeredAt, parseKeywordList(linkedTask.getFeedbackKeywordText()));
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_MANUAL_CONFIRM.equalsIgnoreCase(feedbackRule)) {
            return null;
        }
        return findFirstFeedbackAfter(patient, triggeredAt);
    }

    private PatientChatMessage findFirstKeywordFeedbackAfter(Patient patient, LocalDateTime triggeredAt, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return findFirstFeedbackAfter(patient, triggeredAt);
        }
        return patientChatMessageRepository.findTop20ByPatientPatientIdOrderByMessageTimeDescCreatedAtDesc(resolvePatientIdentity(patient))
            .stream()
            .filter(message -> "INBOUND".equalsIgnoreCase(message.getDirection()))
            .filter(message -> {
                LocalDateTime time = message.getMessageTime() == null ? message.getCreatedAt() : message.getMessageTime();
                return !time.isBefore(triggeredAt);
            })
            .filter(message -> keywords.stream().anyMatch(keyword -> containsIgnoreCase(message.getContent(), keyword)))
            .min(Comparator.comparing(message -> message.getMessageTime() == null ? message.getCreatedAt() : message.getMessageTime()))
            .orElse(null);
    }

    private List<String> parseKeywordList(String keywordText) {
        String normalized = trimToNull(keywordText);
        if (normalized == null) {
            return List.of();
        }
        return java.util.Arrays.stream(normalized.split("[,，\\n\\r]+"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .distinct()
            .toList();
    }

    private String resolveFeedbackWaitingSummary(MessageTriggerRule linkedTask) {
        if (linkedTask == null) {
            return "消息已发送，等待患者反馈";
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_KEYWORD.equalsIgnoreCase(linkedTask.getFeedbackRule())) {
            return "消息已发送，等待患者关键词反馈";
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_MANUAL_CONFIRM.equalsIgnoreCase(linkedTask.getFeedbackRule())) {
            return "消息已发送，等待人工确认反馈";
        }
        return "消息已发送，等待患者反馈";
    }

    private String resolveFeedbackWaitingReason(MessageTriggerRule linkedTask) {
        if (linkedTask == null) {
            return "等待患者群聊反馈";
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_KEYWORD.equalsIgnoreCase(linkedTask.getFeedbackRule())) {
            return "等待患者群聊命中反馈关键词";
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_MANUAL_CONFIRM.equalsIgnoreCase(linkedTask.getFeedbackRule())) {
            return "等待人工确认该任务已反馈";
        }
        return "等待患者群聊反馈";
    }

    private String resolveFeedbackSuccessReason(MessageTriggerRule linkedTask) {
        if (linkedTask == null) {
            return "消息已发送，已收到反馈";
        }
        if (MessageTriggerRuleService.FEEDBACK_RULE_KEYWORD.equalsIgnoreCase(linkedTask.getFeedbackRule())) {
            return "消息已发送，已命中反馈关键词";
        }
        return "消息已发送，已收到反馈";
    }

    private boolean containsIgnoreCase(String source, String expected) {
        return safe(source).toLowerCase(Locale.ROOT).contains(safe(expected).toLowerCase(Locale.ROOT));
    }
}
