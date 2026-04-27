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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_process_step_instance")
public class PatientProcessStepInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private PatientProcessInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_step_id", nullable = false)
    private ProcessTemplateStep templateStep;

    @Column(nullable = false, length = 64)
    private String stepCode;

    @Column(nullable = false, length = 120)
    private String stepName;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 32)
    private String stepType;

    @Column(nullable = false, length = 32)
    private String triggerMode;

    @Column(nullable = false)
    private Integer relativeDayOffset = 0;

    @Column(length = 32)
    private String relativeBase;

    private LocalDate plannedDate;

    private LocalDateTime plannedAt;

    private LocalDateTime triggeredAt;

    private LocalDateTime completedAt;

    @Column(length = 255)
    private String statusReason;

    @Column(length = 255)
    private String feedbackSummary;

    @Column(length = 64)
    private String linkedQuestionnaireTaskNo;

    @Column(length = 32)
    private String linkedQuestionnaireStatus;

    @Column(length = 32)
    private String linkedAutomationJobNo;

    @Column(length = 32)
    private String linkedAutomationJobStatus;

    @Column(length = 64)
    private String linkedMessageRuleCode;

    @Column(length = 255)
    private String displayHint;

    @Column(nullable = false)
    private Boolean feedbackRequired = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    public PatientProcessInstance getInstance() {
        return instance;
    }

    public void setInstance(PatientProcessInstance instance) {
        this.instance = instance;
    }

    public ProcessTemplateStep getTemplateStep() {
        return templateStep;
    }

    public void setTemplateStep(ProcessTemplateStep templateStep) {
        this.templateStep = templateStep;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }

    public Integer getRelativeDayOffset() {
        return relativeDayOffset;
    }

    public void setRelativeDayOffset(Integer relativeDayOffset) {
        this.relativeDayOffset = relativeDayOffset;
    }

    public String getRelativeBase() {
        return relativeBase;
    }

    public void setRelativeBase(String relativeBase) {
        this.relativeBase = relativeBase;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    public String getLinkedQuestionnaireTaskNo() {
        return linkedQuestionnaireTaskNo;
    }

    public void setLinkedQuestionnaireTaskNo(String linkedQuestionnaireTaskNo) {
        this.linkedQuestionnaireTaskNo = linkedQuestionnaireTaskNo;
    }

    public String getLinkedQuestionnaireStatus() {
        return linkedQuestionnaireStatus;
    }

    public void setLinkedQuestionnaireStatus(String linkedQuestionnaireStatus) {
        this.linkedQuestionnaireStatus = linkedQuestionnaireStatus;
    }

    public String getLinkedAutomationJobNo() {
        return linkedAutomationJobNo;
    }

    public void setLinkedAutomationJobNo(String linkedAutomationJobNo) {
        this.linkedAutomationJobNo = linkedAutomationJobNo;
    }

    public String getLinkedAutomationJobStatus() {
        return linkedAutomationJobStatus;
    }

    public void setLinkedAutomationJobStatus(String linkedAutomationJobStatus) {
        this.linkedAutomationJobStatus = linkedAutomationJobStatus;
    }

    public String getLinkedMessageRuleCode() {
        return linkedMessageRuleCode;
    }

    public void setLinkedMessageRuleCode(String linkedMessageRuleCode) {
        this.linkedMessageRuleCode = linkedMessageRuleCode;
    }

    public String getDisplayHint() {
        return displayHint;
    }

    public void setDisplayHint(String displayHint) {
        this.displayHint = displayHint;
    }

    public Boolean getFeedbackRequired() {
        return feedbackRequired;
    }

    public void setFeedbackRequired(Boolean feedbackRequired) {
        this.feedbackRequired = feedbackRequired;
    }
}
