package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record MessageTriggerRuleView(
    Long id,
    String ruleCode,
    String ruleName,
    String ruleMode,
    String taskCategory,
    String triggerType,
    Integer relativeDayOffset,
    String keywordText,
    String keywordMatchMode,
    String conditionRelation,
    List<MessageTriggerRuleConditionView> conditions,
    String targetType,
    String customTargetConversation,
    List<MessageTriggerRuleContentBlockView> contentBlocks,
    Boolean feedbackRequired,
    String feedbackRule,
    String feedbackKeywordText,
    Integer feedbackTimeoutHours,
    Boolean enabled,
    Integer sortOrder,
    String description,
    LocalDateTime lastTriggeredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
