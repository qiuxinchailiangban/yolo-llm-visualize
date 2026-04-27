package com.hospital.followup.dto.admin;

public record MessageTriggerRuleContentBlockRequest(
    String blockType,
    String textContent,
    String mediaPath,
    String mediaName,
    Integer sortOrder
) {
}
