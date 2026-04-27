package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_trigger_rule")
public class MessageTriggerRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String ruleCode;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 16)
    private String ruleMode;

    @Column(nullable = false, length = 32)
    private String taskCategory = "PROCESS";

    @Column(nullable = false, length = 32)
    private String triggerType;

    @Column(columnDefinition = "TEXT")
    private String triggerConfigJson;

    @Column(nullable = false, length = 16)
    private String conditionRelation = "ALL";

    @Column(columnDefinition = "TEXT")
    private String conditionConfigJson;

    @Column(nullable = false, length = 32)
    private String targetType;

    @Column(length = 128)
    private String customTargetConversation;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contentConfigJson;

    @Column(nullable = false)
    private Boolean feedbackRequired = Boolean.FALSE;

    @Column(length = 32)
    private String feedbackRule = "NONE";

    @Column(columnDefinition = "TEXT")
    private String feedbackKeywordText;

    private Integer feedbackTimeoutHours;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(nullable = false)
    private Integer sortOrder = 100;

    @Column(length = 255)
    private String description;

    private LocalDateTime lastTriggeredAt;

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleMode() {
        return ruleMode;
    }

    public void setRuleMode(String ruleMode) {
        this.ruleMode = ruleMode;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTaskCategory() {
        return taskCategory;
    }

    public void setTaskCategory(String taskCategory) {
        this.taskCategory = taskCategory;
    }

    public String getTriggerConfigJson() {
        return triggerConfigJson;
    }

    public void setTriggerConfigJson(String triggerConfigJson) {
        this.triggerConfigJson = triggerConfigJson;
    }

    public String getConditionRelation() {
        return conditionRelation;
    }

    public void setConditionRelation(String conditionRelation) {
        this.conditionRelation = conditionRelation;
    }

    public String getConditionConfigJson() {
        return conditionConfigJson;
    }

    public void setConditionConfigJson(String conditionConfigJson) {
        this.conditionConfigJson = conditionConfigJson;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getCustomTargetConversation() {
        return customTargetConversation;
    }

    public void setCustomTargetConversation(String customTargetConversation) {
        this.customTargetConversation = customTargetConversation;
    }

    public String getContentConfigJson() {
        return contentConfigJson;
    }

    public void setContentConfigJson(String contentConfigJson) {
        this.contentConfigJson = contentConfigJson;
    }

    public Boolean getFeedbackRequired() {
        return feedbackRequired;
    }

    public void setFeedbackRequired(Boolean feedbackRequired) {
        this.feedbackRequired = feedbackRequired;
    }

    public String getFeedbackRule() {
        return feedbackRule;
    }

    public void setFeedbackRule(String feedbackRule) {
        this.feedbackRule = feedbackRule;
    }

    public String getFeedbackKeywordText() {
        return feedbackKeywordText;
    }

    public void setFeedbackKeywordText(String feedbackKeywordText) {
        this.feedbackKeywordText = feedbackKeywordText;
    }

    public Integer getFeedbackTimeoutHours() {
        return feedbackTimeoutHours;
    }

    public void setFeedbackTimeoutHours(Integer feedbackTimeoutHours) {
        this.feedbackTimeoutHours = feedbackTimeoutHours;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }
}
