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

@Entity
@Table(name = "process_template_step")
public class ProcessTemplateStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProcessTemplate template;

    @Column(nullable = false, length = 64)
    private String stepCode;

    @Column(nullable = false, length = 120)
    private String stepName;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 32)
    private String stepType;

    @Column(nullable = false, length = 32)
    private String triggerMode;

    @Column(nullable = false)
    private Integer relativeDayOffset = 0;

    @Column(length = 32)
    private String relativeBase;

    @Column(length = 255)
    private String description;

    @Column(length = 64)
    private String messageRuleCode;

    @Column(length = 64)
    private String stageCode;

    @Column(length = 64)
    private String templateCode;

    @Column(length = 255)
    private String completionRule;

    @Column(length = 255)
    private String applicableSurgeryTags;

    @Column(nullable = false)
    private Boolean feedbackRequired = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    public Long getId() {
        return id;
    }

    public ProcessTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ProcessTemplate template) {
        this.template = template;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessageRuleCode() {
        return messageRuleCode;
    }

    public void setMessageRuleCode(String messageRuleCode) {
        this.messageRuleCode = messageRuleCode;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getCompletionRule() {
        return completionRule;
    }

    public void setCompletionRule(String completionRule) {
        this.completionRule = completionRule;
    }

    public String getApplicableSurgeryTags() {
        return applicableSurgeryTags;
    }

    public void setApplicableSurgeryTags(String applicableSurgeryTags) {
        this.applicableSurgeryTags = applicableSurgeryTags;
    }

    public Boolean getFeedbackRequired() {
        return feedbackRequired;
    }

    public void setFeedbackRequired(Boolean feedbackRequired) {
        this.feedbackRequired = feedbackRequired;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
