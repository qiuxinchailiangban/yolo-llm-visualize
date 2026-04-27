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
@Table(name = "patient_process_instance")
public class PatientProcessInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProcessTemplate template;

    @Column(nullable = false, unique = true, length = 32)
    private String instanceNo;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 64)
    private String currentStepCode;

    @Column(length = 120)
    private String currentStepName;

    @Column(nullable = false)
    private Integer totalStepCount = 0;

    @Column(nullable = false)
    private Integer completedStepCount = 0;

    @Column(nullable = false)
    private Integer waitingFeedbackCount = 0;

    @Column(nullable = false)
    private Integer warningStepCount = 0;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(length = 255)
    private String summaryText;

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public ProcessTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ProcessTemplate template) {
        this.template = template;
    }

    public String getInstanceNo() {
        return instanceNo;
    }

    public void setInstanceNo(String instanceNo) {
        this.instanceNo = instanceNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStepCode() {
        return currentStepCode;
    }

    public void setCurrentStepCode(String currentStepCode) {
        this.currentStepCode = currentStepCode;
    }

    public String getCurrentStepName() {
        return currentStepName;
    }

    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
    }

    public Integer getTotalStepCount() {
        return totalStepCount;
    }

    public void setTotalStepCount(Integer totalStepCount) {
        this.totalStepCount = totalStepCount;
    }

    public Integer getCompletedStepCount() {
        return completedStepCount;
    }

    public void setCompletedStepCount(Integer completedStepCount) {
        this.completedStepCount = completedStepCount;
    }

    public Integer getWaitingFeedbackCount() {
        return waitingFeedbackCount;
    }

    public void setWaitingFeedbackCount(Integer waitingFeedbackCount) {
        this.waitingFeedbackCount = waitingFeedbackCount;
    }

    public Integer getWarningStepCount() {
        return warningStepCount;
    }

    public void setWarningStepCount(Integer warningStepCount) {
        this.warningStepCount = warningStepCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}
