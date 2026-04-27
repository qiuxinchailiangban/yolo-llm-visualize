package com.hospital.followup.dto.admin;

public record MessageTriggerRuleManualExecuteItemView(
    String candidateKey,
    String ruleName,
    String patientId,
    String patientName,
    String status,
    String message
) {
}
