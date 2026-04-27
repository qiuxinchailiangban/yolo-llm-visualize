package com.hospital.followup.dto.publicapi;

public record PublicTemplatePayload(
    String templateCode,
    String templateName,
    String schemaJson
) {
}
