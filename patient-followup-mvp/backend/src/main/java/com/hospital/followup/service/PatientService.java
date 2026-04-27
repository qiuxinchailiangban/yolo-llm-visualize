package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.MessageTriggerExecution;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.PatientStatus;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import com.hospital.followup.dto.admin.CreatePatientRequest;
import com.hospital.followup.dto.admin.PatientImportResult;
import com.hospital.followup.dto.admin.PatientImportRowResult;
import com.hospital.followup.dto.admin.PatientDetailView;
import com.hospital.followup.dto.admin.PatientTaskRebuildResult;
import com.hospital.followup.dto.admin.PatientTaskView;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.dto.publicapi.IntakeSubmissionRequest;
import com.hospital.followup.dto.publicapi.IntakeSubmissionResult;
import com.hospital.followup.repository.AutomationJobRepository;
import com.hospital.followup.repository.FollowupStageRepository;
import com.hospital.followup.repository.MessageTriggerExecutionRepository;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.PatientChatMessageRepository;
import com.hospital.followup.repository.PatientProcessInstanceRepository;
import com.hospital.followup.repository.PatientProcessStepInstanceRepository;
import com.hospital.followup.repository.QuestionnaireQrCodeRepository;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import com.hospital.followup.repository.ReminderTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final QuestionnaireTaskRepository questionnaireTaskRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final FollowupStageRepository stageRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final ReminderTaskRepository reminderTaskRepository;
    private final AutomationJobRepository automationJobRepository;
    private final MessageTriggerExecutionRepository messageTriggerExecutionRepository;
    private final PatientChatMessageRepository patientChatMessageRepository;
    private final PatientProcessInstanceRepository patientProcessInstanceRepository;
    private final PatientProcessStepInstanceRepository patientProcessStepInstanceRepository;
    private final QuestionnaireQrCodeRepository questionnaireQrCodeRepository;
    private final PatientChatMessageService patientChatMessageService;
    private final PatientIdGenerator patientIdGenerator;
    private final TaskNumberGenerator taskNumberGenerator;
    private final ObjectMapper objectMapper;

    public PatientService(
        PatientRepository patientRepository,
        QuestionnaireTaskRepository questionnaireTaskRepository,
        QuestionnaireTemplateRepository templateRepository,
        FollowupStageRepository stageRepository,
        QuestionnaireResponseRepository responseRepository,
        ReminderTaskRepository reminderTaskRepository,
        AutomationJobRepository automationJobRepository,
        MessageTriggerExecutionRepository messageTriggerExecutionRepository,
        PatientChatMessageRepository patientChatMessageRepository,
        PatientProcessInstanceRepository patientProcessInstanceRepository,
        PatientProcessStepInstanceRepository patientProcessStepInstanceRepository,
        QuestionnaireQrCodeRepository questionnaireQrCodeRepository,
        PatientChatMessageService patientChatMessageService,
        PatientIdGenerator patientIdGenerator,
        TaskNumberGenerator taskNumberGenerator,
        ObjectMapper objectMapper
    ) {
        this.patientRepository = patientRepository;
        this.questionnaireTaskRepository = questionnaireTaskRepository;
        this.templateRepository = templateRepository;
        this.stageRepository = stageRepository;
        this.responseRepository = responseRepository;
        this.reminderTaskRepository = reminderTaskRepository;
        this.automationJobRepository = automationJobRepository;
        this.messageTriggerExecutionRepository = messageTriggerExecutionRepository;
        this.patientChatMessageRepository = patientChatMessageRepository;
        this.patientProcessInstanceRepository = patientProcessInstanceRepository;
        this.patientProcessStepInstanceRepository = patientProcessStepInstanceRepository;
        this.questionnaireQrCodeRepository = questionnaireQrCodeRepository;
        this.patientChatMessageService = patientChatMessageService;
        this.patientIdGenerator = patientIdGenerator;
        this.taskNumberGenerator = taskNumberGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PatientView> listPatients(String keyword, PatientStatus status) {
        List<Patient> patients;
        if (keyword != null && !keyword.isBlank()) {
            patients = patientRepository.findByNameContainingIgnoreCaseOrPatientIdContainingIgnoreCaseOrderByCreatedAtDesc(keyword, keyword);
        } else if (status != null) {
            patients = patientRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            patients = patientRepository.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
        }
        return patients.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public PatientDetailView getPatientDetail(String patientId) {
        Patient patient = patientRepository.findByPatientId(patientId)
            .or(() -> patientRepository.findByPatientNo(patientId))
            .orElseThrow(() -> new EntityNotFoundException("患者不存在"));
        List<PatientTaskView> tasks = questionnaireTaskRepository.findByPatientPatientIdOrderByDueDateAsc(patientId)
            .stream()
            .map(task -> new PatientTaskView(
                task.getTaskNo(),
                task.getStage().getStageCode(),
                task.getStage().getStageName(),
                task.getTemplate().getTemplateName(),
                task.getStatus().name(),
                task.getDueDate(),
                task.getFinishedAt()
            ))
            .toList();
        return new PatientDetailView(toView(patient), tasks, patientChatMessageService.listRecentMessages(patient.getPatientId()));
    }

    @Transactional
    public PatientView createPatient(CreatePatientRequest request) {
        Patient patient = new Patient();
        assignPatientIdentifiers(patient, patientIdGenerator.nextId());
        applyPatientChanges(patient, request, "ADMIN");
        patient.setStatus(PatientStatus.ACTIVE);
        patientRepository.save(patient);
        createFollowupTasks(patient);
        return toView(patient);
    }

    @Transactional
    public PatientView updatePatient(String patientId, CreatePatientRequest request) {
        Patient patient = loadPatient(patientId);
        LocalDate originalSurgeryDate = patient.getSurgeryDate();
        if (originalSurgeryDate != null && request.surgeryDate() == null) {
            throw new IllegalArgumentException("已有手术日期和随访任务的患者不能直接清空手术日期");
        }

        applyPatientChanges(patient, request, patient.getSourceChannel() == null ? "ADMIN" : patient.getSourceChannel());
        if (!Objects.equals(originalSurgeryDate, patient.getSurgeryDate())) {
            rebuildFollowupTasks(patient);
        }
        return toView(patient);
    }

    @Transactional
    public void deletePatient(String patientId) {
        Patient patient = loadPatient(patientId);
        Long pk = patient.getId();

        List<MessageTriggerExecution> triggerExecutions = messageTriggerExecutionRepository.findByPatient_Id(pk);
        List<Long> triggerExecutionIds = triggerExecutions.stream().map(MessageTriggerExecution::getId).toList();

        List<QuestionnaireTask> tasks = questionnaireTaskRepository.findByPatient_Id(pk);
        List<Long> taskIds = tasks.stream().map(QuestionnaireTask::getId).toList();

        patientChatMessageRepository.deleteByPatient_Id(pk);
        patientProcessInstanceRepository.findByPatient_Id(pk).ifPresent(instance -> {
            patientProcessStepInstanceRepository.deleteByInstance_Id(instance.getId());
            patientProcessStepInstanceRepository.flush();
            patientProcessInstanceRepository.delete(instance);
            patientProcessInstanceRepository.flush();
        });
        if (!triggerExecutionIds.isEmpty()) {
            automationJobRepository.deleteAll(
                automationJobRepository.findByBizTypeAndBizIdIn("MESSAGE_TRIGGER_EXECUTION", triggerExecutionIds)
            );
            messageTriggerExecutionRepository.deleteByPatient_Id(pk);
        }

        responseRepository.deleteByPatient_Id(pk);
        if (!taskIds.isEmpty()) {
            questionnaireQrCodeRepository.deleteByTask_IdIn(taskIds);

            List<Long> reminderIds = reminderTaskRepository.findByQuestionnaireTaskIdIn(taskIds)
                .stream()
                .map(reminderTask -> reminderTask.getId())
                .toList();
            if (!reminderIds.isEmpty()) {
                automationJobRepository.deleteAll(
                    automationJobRepository.findByBizTypeAndBizIdIn("REMINDER_TASK", reminderIds)
                );
            }
            reminderTaskRepository.deleteByQuestionnaireTaskIdIn(taskIds);
            questionnaireTaskRepository.deleteByPatient_Id(pk);
        }
        questionnaireTaskRepository.flush();
        responseRepository.flush();
        patientChatMessageRepository.flush();
        messageTriggerExecutionRepository.flush();
        patientRepository.delete(patient);
    }

    @Transactional
    public PatientTaskRebuildResult rebuildAllPatientTasks() {
        List<Patient> patients = patientRepository.findAll();
        int rebuiltPatients = 0;
        int skippedPatients = 0;
        int totalTasksAffected = 0;

        for (Patient patient : patients) {
            if (patient.getSurgeryDate() == null) {
                skippedPatients++;
                continue;
            }
            List<String> affectedTaskNos = rebuildFollowupTasks(patient);
            rebuiltPatients++;
            totalTasksAffected += affectedTaskNos.size();
        }

        return new PatientTaskRebuildResult(
            patients.size(),
            rebuiltPatients,
            skippedPatients,
            totalTasksAffected
        );
    }

    @Transactional
    public IntakeSubmissionResult submitIntake(IntakeSubmissionRequest request) {
        Patient patient = new Patient();
        assignPatientIdentifiers(patient, patientIdGenerator.nextId());
        patient.setName(normalizeText(request.name()));
        patient.setGender(normalizeText(request.gender()));
        patient.setPhone(normalizeText(request.phone()));
        patient.setBirthDate(request.birthDate());
        patient.setSurgeryDate(request.surgeryDate());
        patient.setDiagnosis(normalizeText(request.diagnosis()));
        patient.setSourceChannel("WECHAT_MINIAPP");
        patient.setStatus(PatientStatus.ACTIVE);
        patientRepository.save(patient);

        QuestionnaireTemplate intakeTemplate = templateRepository
            .findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType.INTAKE, TemplateStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("未配置启用中的首诊模板"));

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setPatient(patient);
        response.setTemplate(intakeTemplate);
        response.setTask(null);
        response.setAnswersJson(writeJson(request.answers() == null ? Map.of() : request.answers()));
        response.setSubmitChannel("WECHAT_MINIAPP");
        response.setSubmittedAt(LocalDateTime.now());
        responseRepository.save(response);

        List<String> createdTaskNos = createFollowupTasks(patient);
        return new IntakeSubmissionResult(patient.getPatientId(), createdTaskNos, "提交成功，已生成后续随访任务");
    }

    @Transactional
    public PatientImportResult importPatientsFromCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new IllegalArgumentException("CSV 内容不能为空");
        }

        List<String> lines = csvContent.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
        if (lines.size() <= 1) {
            throw new IllegalArgumentException("CSV 至少需要表头和一行数据");
        }

        List<PatientImportRowResult> rowResults = new ArrayList<>();
        int successRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int totalTasksGenerated = 0;

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            String[] columns = splitCsvLine(line);
            if (columns.length < 3) {
                skippedCount++;
                rowResults.add(new PatientImportRowResult(index + 1, "", "", "", "SKIPPED", null, 0, "列数不足，至少需要姓名、手机号、手术日期"));
                continue;
            }

            String name = normalizeText(columns[0]);
            String phone = normalizeText(columns[1]);
            String surgeryDateText = normalizeText(columns[2]);
            String gender = getColumn(columns, 3);
            String birthDateText = getColumn(columns, 4);
            String diagnosis = getColumn(columns, 5);
            String sourceChannel = getColumn(columns, 6);
            String surgeryScheduleTag = getColumn(columns, 7);
            String surgeryTimeText = getColumn(columns, 8);

            if (name == null || surgeryDateText == null) {
                skippedCount++;
                rowResults.add(new PatientImportRowResult(index + 1, Objects.toString(name, ""), Objects.toString(phone, ""), Objects.toString(surgeryDateText, ""), "SKIPPED", null, 0, "姓名和手术日期不能为空"));
                continue;
            }

            try {
                Patient patient = matchPatient(name, phone, parseDate(surgeryDateText));
                boolean created = false;
                if (patient == null) {
                    patient = new Patient();
                    assignPatientIdentifiers(patient, patientIdGenerator.nextId());
                    created = true;
                }

                patient.setName(name);
                patient.setPhone(phone);
                patient.setGender(gender);
                patient.setBirthDate(parseDateNullable(birthDateText));
                patient.setSurgeryDate(parseDate(surgeryDateText));
                patient.setSurgeryScheduleTag(surgeryScheduleTag);
                patient.setSurgeryTimeText(surgeryTimeText);
                patient.setDiagnosis(diagnosis);
                patient.setSourceChannel(sourceChannel == null ? "CSV_IMPORT" : sourceChannel);
                patient.setStatus(PatientStatus.ACTIVE);
                patientRepository.save(patient);

                List<String> generatedTasks = rebuildFollowupTasks(patient);
                totalTasksGenerated += generatedTasks.size();
                successRows++;
                if (created) {
                    createdCount++;
                } else {
                    updatedCount++;
                }
                rowResults.add(
                    new PatientImportRowResult(
                        index + 1,
                        patient.getName(),
                        patient.getPhone(),
                        patient.getSurgeryDate() == null ? null : patient.getSurgeryDate().toString(),
                        created ? "CREATED" : "UPDATED",
                        patient.getPatientId(),
                        generatedTasks.size(),
                        created ? "已创建患者并生成任务" : "已更新患者并重建任务"
                    )
                );
            } catch (Exception error) {
                skippedCount++;
                rowResults.add(
                    new PatientImportRowResult(
                        index + 1,
                        Objects.toString(name, ""),
                        Objects.toString(phone, ""),
                        Objects.toString(surgeryDateText, ""),
                        "SKIPPED",
                        null,
                        0,
                        error.getMessage()
                    )
                );
            }
        }

        return new PatientImportResult(
            lines.size() - 1,
            successRows,
            createdCount,
            updatedCount,
            skippedCount,
            totalTasksGenerated,
            rowResults
        );
    }

    @Transactional
    public List<String> createFollowupTasks(Patient patient) {
        if (patient.getSurgeryDate() == null) {
            return List.of();
        }
        List<String> taskNos = new ArrayList<>();
        List<FollowupStage> stages = stageRepository.findByEnabledTrueOrderBySortOrderAsc();
        for (FollowupStage stage : stages) {
            QuestionnaireTemplate template = templateRepository
                .findFirstByStageIdAndStatusOrderByUpdatedAtDesc(stage.getId(), TemplateStatus.ACTIVE)
                .orElse(null);
            if (template == null) {
                continue;
            }
            QuestionnaireTask task = new QuestionnaireTask();
            task.setTaskNo(taskNumberGenerator.nextNo());
            task.setPatient(patient);
            task.setStage(stage);
            task.setTemplate(template);
            task.setStatus(QuestionnaireTaskStatus.PENDING);
            task.setDueDate(patient.getSurgeryDate().plusDays(stage.getDayOffset()));
            questionnaireTaskRepository.save(task);
            taskNos.add(task.getTaskNo());
        }
        patient.setStatus(taskNos.isEmpty() ? PatientStatus.ACTIVE : PatientStatus.FOLLOWING);
        return taskNos;
    }

    @Transactional
    public List<String> rebuildFollowupTasks(Patient patient) {
        if (patient.getSurgeryDate() == null) {
            return List.of();
        }

        List<String> affectedTaskNos = new ArrayList<>();
        boolean hasTrackedTasks = false;
        Map<Long, List<QuestionnaireTask>> tasksByStage = new HashMap<>();
        String patientIdentity = resolvePatientIdentity(patient);
        for (QuestionnaireTask task : questionnaireTaskRepository.findByPatientPatientId(patientIdentity)) {
            tasksByStage.computeIfAbsent(task.getStage().getId(), key -> new ArrayList<>()).add(task);
        }

        Set<Long> enabledStageIds = new HashSet<>();
        for (FollowupStage stage : stageRepository.findByEnabledTrueOrderBySortOrderAsc()) {
            QuestionnaireTemplate template = templateRepository
                .findFirstByStageIdAndStatusOrderByUpdatedAtDesc(stage.getId(), TemplateStatus.ACTIVE)
                .orElse(null);
            if (template == null) {
                continue;
            }

            enabledStageIds.add(stage.getId());
            LocalDate dueDate = patient.getSurgeryDate().plusDays(stage.getDayOffset());
            List<QuestionnaireTask> stageTasks = tasksByStage.getOrDefault(stage.getId(), List.of());
            if (hasCompletedTask(stageTasks)) {
                hasTrackedTasks = true;
                for (QuestionnaireTask stageTask : stageTasks) {
                    if (stageTask.getStatus() != QuestionnaireTaskStatus.COMPLETED) {
                        stageTask.setStatus(QuestionnaireTaskStatus.CANCELLED);
                    }
                }
                continue;
            }

            QuestionnaireTask reusableTask = null;
            for (QuestionnaireTask stageTask : stageTasks) {
                if (reusableTask == null && stageTask.getStatus() != QuestionnaireTaskStatus.CANCELLED) {
                    reusableTask = stageTask;
                }
            }

            if (reusableTask == null) {
                QuestionnaireTask task = new QuestionnaireTask();
                task.setTaskNo(taskNumberGenerator.nextNo());
                task.setPatient(patient);
                task.setStage(stage);
                task.setTemplate(template);
                task.setStatus(QuestionnaireTaskStatus.PENDING);
                task.setDueDate(dueDate);
                task.setFinishedAt(null);
                questionnaireTaskRepository.save(task);
                affectedTaskNos.add(task.getTaskNo());
                hasTrackedTasks = true;
            } else {
                reusableTask.setStage(stage);
                reusableTask.setTemplate(template);
                reusableTask.setDueDate(dueDate);
                reusableTask.setStatus(QuestionnaireTaskStatus.PENDING);
                reusableTask.setFinishedAt(null);
                affectedTaskNos.add(reusableTask.getTaskNo());
                hasTrackedTasks = true;

                for (QuestionnaireTask extraTask : stageTasks) {
                    if (!extraTask.getId().equals(reusableTask.getId()) && extraTask.getStatus() != QuestionnaireTaskStatus.COMPLETED) {
                        extraTask.setStatus(QuestionnaireTaskStatus.CANCELLED);
                    }
                }
            }
        }

        for (List<QuestionnaireTask> stageTasks : tasksByStage.values()) {
            for (QuestionnaireTask task : stageTasks) {
                if (!enabledStageIds.contains(task.getStage().getId()) && task.getStatus() != QuestionnaireTaskStatus.COMPLETED) {
                    task.setStatus(QuestionnaireTaskStatus.CANCELLED);
                }
            }
        }

        patient.setStatus(hasTrackedTasks ? PatientStatus.FOLLOWING : PatientStatus.ACTIVE);
        return affectedTaskNos;
    }

    private PatientView toView(Patient patient) {
        String patientIdentity = resolvePatientIdentity(patient);
        return new PatientView(
            patientIdentity,
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
            patient.getStatus().name(),
            patient.getCreatedAt()
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("问卷答案JSON格式错误");
        }
    }

    private Patient matchPatient(String name, String phone, java.time.LocalDate surgeryDate) {
        if (phone != null && !phone.isBlank()) {
            List<Patient> exactMatches = patientRepository.findByNameAndPhoneAndSurgeryDate(name, phone, surgeryDate);
            if (!exactMatches.isEmpty()) {
                return exactMatches.getFirst();
            }
        }
        List<Patient> fallbackMatches = patientRepository.findByNameAndSurgeryDate(name, surgeryDate);
        return fallbackMatches.isEmpty() ? null : fallbackMatches.getFirst();
    }

    private boolean hasCompletedTask(List<QuestionnaireTask> tasks) {
        return tasks.stream().anyMatch(task -> task.getStatus() == QuestionnaireTaskStatus.COMPLETED);
    }

    private java.time.LocalDate parseDate(String value) {
        try {
            return java.time.LocalDate.parse(value);
        } catch (Exception error) {
            throw new IllegalArgumentException("日期格式错误，应为 YYYY-MM-DD");
        }
    }

    private java.time.LocalDate parseDateNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String getColumn(String[] columns, int index) {
        return index < columns.length ? normalizeText(columns[index]) : null;
    }

    private Patient loadPatient(String patientId) {
        return patientRepository.findByPatientId(patientId)
            .or(() -> patientRepository.findByPatientNo(patientId))
            .orElseThrow(() -> new EntityNotFoundException("患者不存在"));
    }

    private void applyPatientChanges(Patient patient, CreatePatientRequest request, String defaultSourceChannel) {
        patient.setName(normalizeText(request.name()));
        patient.setGender(normalizeText(request.gender()));
        patient.setPhone(normalizeText(request.phone()));
        patient.setBirthDate(request.birthDate());
        patient.setSurgeryDate(request.surgeryDate());
        patient.setSurgeryScheduleTag(normalizeText(request.surgeryScheduleTag()));
        patient.setSurgeryTimeText(normalizeText(request.surgeryTimeText()));
        patient.setDiagnosis(normalizeText(request.diagnosis()));
        patient.setSourceChannel(
            request.sourceChannel() == null || request.sourceChannel().isBlank() ? defaultSourceChannel : request.sourceChannel()
        );
    }

    private void assignPatientIdentifiers(Patient patient, String patientIdentity) {
        patient.setPatientId(patientIdentity);
        patient.setPatientNo(patientIdentity);
    }

    private String resolvePatientIdentity(Patient patient) {
        if (patient.getPatientId() != null && !patient.getPatientId().isBlank()) {
            return patient.getPatientId();
        }
        return patient.getPatientNo();
    }

    private String[] splitCsvLine(String line) {
        return Arrays.stream(line.split(",", -1))
            .map(value -> value == null ? "" : value.replace("\"", "").trim())
            .toArray(String[]::new);
    }
}
