package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.hospital.followup.persistence.QuestionnaireTaskStatusConverter;

@Entity
@Table(name = "questionnaire_task")
public class QuestionnaireTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String taskNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private FollowupStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

    @Convert(converter = QuestionnaireTaskStatusConverter.class)
    @Column(nullable = false, length = 32)
    private QuestionnaireTaskStatus status;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public FollowupStage getStage() {
        return stage;
    }

    public void setStage(FollowupStage stage) {
        this.stage = stage;
    }

    public QuestionnaireTemplate getTemplate() {
        return template;
    }

    public void setTemplate(QuestionnaireTemplate template) {
        this.template = template;
    }

    public QuestionnaireTaskStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionnaireTaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
