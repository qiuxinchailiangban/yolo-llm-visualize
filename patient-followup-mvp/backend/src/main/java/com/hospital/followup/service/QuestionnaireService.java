package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import com.hospital.followup.dto.publicapi.PublicTaskDetailView;
import com.hospital.followup.dto.publicapi.PublicTemplatePayload;
import com.hospital.followup.dto.publicapi.TaskSubmissionRequest;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionnaireService {

    private final QuestionnaireTaskRepository taskRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public QuestionnaireService(
        QuestionnaireTaskRepository taskRepository,
        QuestionnaireResponseRepository responseRepository,
        QuestionnaireTemplateRepository templateRepository,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.responseRepository = responseRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PublicTemplatePayload getIntakeTemplate() {
        QuestionnaireTemplate template = templateRepository
            .findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType.INTAKE, TemplateStatus.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("首诊模板不存在"));
        return new PublicTemplatePayload(template.getTemplateCode(), template.getTemplateName(), template.getSchemaJson());
    }

    @Transactional
    public void refreshOverdueTasks() {
        for (QuestionnaireTask task : taskRepository.findByDueDateLessThanAndStatus(LocalDate.now(), QuestionnaireTaskStatus.PENDING)) {
            task.setStatus(QuestionnaireTaskStatus.OVERDUE);
        }
    }

    @Transactional(readOnly = true)
    public PublicTaskDetailView getTaskDetail(String taskNo) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        return new PublicTaskDetailView(
            task.getTaskNo(),
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            task.getStage().getStageName(),
            task.getDueDate().toString(),
            task.getStatus().name(),
            new PublicTemplatePayload(
                task.getTemplate().getTemplateCode(),
                task.getTemplate().getTemplateName(),
                task.getTemplate().getSchemaJson()
            )
        );
    }

    @Transactional
    public void submitTask(String taskNo, TaskSubmissionRequest request) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        if (task.getStatus() == QuestionnaireTaskStatus.COMPLETED) {
            throw new IllegalArgumentException("该任务已填写完成");
        }
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setPatient(task.getPatient());
        response.setTask(task);
        response.setTemplate(task.getTemplate());
        response.setAnswersJson(writeJson(request.answers()));
        response.setSubmitChannel("WECHAT_MINIAPP");
        response.setSubmittedAt(LocalDateTime.now());
        responseRepository.save(response);

        task.setStatus(QuestionnaireTaskStatus.COMPLETED);
        task.setFinishedAt(LocalDateTime.now());
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("问卷答案JSON格式错误");
        }
    }
}
