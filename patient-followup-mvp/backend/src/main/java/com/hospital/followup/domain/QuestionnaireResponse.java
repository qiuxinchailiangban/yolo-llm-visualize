package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_response")
public class QuestionnaireResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private QuestionnaireTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(length = 32)
    private String submitChannel;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public QuestionnaireTask getTask() {
        return task;
    }

    public void setTask(QuestionnaireTask task) {
        this.task = task;
    }

    public QuestionnaireTemplate getTemplate() {
        return template;
    }

    public void setTemplate(QuestionnaireTemplate template) {
        this.template = template;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public String getSubmitChannel() {
        return submitChannel;
    }

    public void setSubmitChannel(String submitChannel) {
        this.submitChannel = submitChannel;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
