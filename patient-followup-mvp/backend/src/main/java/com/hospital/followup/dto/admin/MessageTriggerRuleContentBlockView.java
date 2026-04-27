package com.hospital.followup.dto.admin;

public record MessageTriggerRuleContentBlockView(
    String blockType,
    String textContent,
    String mediaPath,
    String mediaName,
    Integer sortOrder
) {
}
