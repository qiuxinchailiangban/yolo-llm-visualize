package com.hospital.followup.dto.admin;

public record MessageTriggerRuleManualCandidateView(
    String candidateKey,
    Long ruleId,
    String ruleName,
    String triggerType,
    String patientId,
    String patientName,
    String targetConversation,
    String contentPreview,
    String sourceMessageKey,
    String sourceMessagePreview,
    String detectedReason
) {
}
