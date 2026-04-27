package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MessageTriggerRuleUpsertRequest(
    String ruleCode,
    @NotBlank(message = "任务名称不能为空") String ruleName,
    @NotBlank(message = "规则模式不能为空") String ruleMode,
    @NotBlank(message = "任务分类不能为空") String taskCategory,
    @NotBlank(message = "触发类型不能为空") String triggerType,
    Integer relativeDayOffset,
    String keywordText,
    String keywordMatchMode,
    String conditionRelation,
    List<MessageTriggerRuleConditionRequest> conditions,
    @NotBlank(message = "发送对象不能为空") String targetType,
    String customTargetConversation,
    @NotNull(message = "内容模块不能为空") List<MessageTriggerRuleContentBlockRequest> contentBlocks,
    Boolean feedbackRequired,
    String feedbackRule,
    String feedbackKeywordText,
    Integer feedbackTimeoutHours,
    Boolean enabled,
    Integer sortOrder,
    String description
) {
}
