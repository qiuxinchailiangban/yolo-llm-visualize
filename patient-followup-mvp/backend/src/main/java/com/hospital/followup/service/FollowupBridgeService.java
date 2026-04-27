package com.hospital.followup.service;

import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.dto.bridge.FollowupTaskBridgeView;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowupBridgeService {

    private final QuestionnaireTaskRepository questionnaireTaskRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    public FollowupBridgeService(
        QuestionnaireTaskRepository questionnaireTaskRepository,
        QuestionnaireResponseRepository questionnaireResponseRepository
    ) {
        this.questionnaireTaskRepository = questionnaireTaskRepository;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
    }

    @Transactional(readOnly = true)
    public FollowupTaskBridgeView getFollowupTask(String taskNo) {
        QuestionnaireTask task = questionnaireTaskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("Questionnaire task not found: " + taskNo));
        QuestionnaireResponse response = questionnaireResponseRepository.findFirstByTaskTaskNoOrderBySubmittedAtDesc(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("Questionnaire response not found for task: " + taskNo));

        Patient patient = task.getPatient();
        FollowupStage stage = task.getStage();
        QuestionnaireTemplate template = task.getTemplate();

        return new FollowupTaskBridgeView(
            new FollowupTaskBridgeView.PatientPayload(
                patient.getPatientId(),
                patient.getPatientNo(),
                patient.getName(),
                patient.getGender(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getSurgeryDate(),
                patient.getDiagnosis(),
                patient.getSourceChannel(),
                patient.getStatus().name()
            ),
            new FollowupTaskBridgeView.TaskPayload(
                task.getTaskNo(),
                task.getStatus().name(),
                task.getDueDate(),
                task.getFinishedAt()
            ),
            new FollowupTaskBridgeView.StagePayload(
                stage.getId(),
                stage.getStageCode(),
                stage.getStageName(),
                stage.getDayOffset(),
                stage.getReminderEnabled()
            ),
            new FollowupTaskBridgeView.TemplatePayload(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getTemplateType().name(),
                template.getVersion(),
                template.getStatus().name(),
                template.getSchemaJson()
            ),
            new FollowupTaskBridgeView.ResponsePayload(
                response.getId(),
                response.getAnswersJson(),
                response.getSubmitChannel(),
                response.getSubmittedAt()
            )
        );
    }
}
