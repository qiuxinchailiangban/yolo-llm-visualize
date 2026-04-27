package com.hospital.followup.dto.publicapi;

import java.time.LocalDateTime;

public record PublicQrCodeResolveView(
    String entryType,
    String token,
    boolean collectPatientInfo,
    String submitMode,
    String taskNo,
    String patientId,
    String patientName,
    String stageId,
    String stageName,
    String dueDate,
    String status,
    String templateType,
    LocalDateTime expiresAt,
    PublicTemplatePayload template
) {
}
