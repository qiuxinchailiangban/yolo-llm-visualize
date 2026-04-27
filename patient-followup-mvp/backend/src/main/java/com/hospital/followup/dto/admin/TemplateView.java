package com.hospital.followup.dto.admin;

public record TemplateView(
    Long id,
    String templateCode,
    String templateName,
    String templateType,
    String version,
    Long stageId,
    String stageName,
    String status,
    String schemaJson,
    String description
) {
}
