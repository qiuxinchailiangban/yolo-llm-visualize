package com.hospital.followup.dto.admin;

public record MessageTriggerRuleManualExecuteItemRequest(
    Long ruleId,
    String patientId,
    String candidateKey,
    String sourceMessageKey
) {
}
