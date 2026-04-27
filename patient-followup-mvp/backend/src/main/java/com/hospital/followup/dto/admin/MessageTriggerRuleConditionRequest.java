package com.hospital.followup.dto.admin;

public record MessageTriggerRuleConditionRequest(
    String conditionType,
    String conditionValue,
    Integer sortOrder
) {
}
