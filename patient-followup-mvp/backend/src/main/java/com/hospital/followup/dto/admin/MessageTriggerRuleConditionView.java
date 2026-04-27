package com.hospital.followup.dto.admin;

public record MessageTriggerRuleConditionView(
    String conditionType,
    String conditionValue,
    Integer sortOrder
) {
}
