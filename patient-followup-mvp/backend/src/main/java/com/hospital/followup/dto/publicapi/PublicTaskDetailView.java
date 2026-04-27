package com.hospital.followup.dto.publicapi;

public record PublicTaskDetailView(
    String taskNo,
    String patientId,
    String patientName,
    String stageName,
    String dueDate,
    String status,
    PublicTemplatePayload template
) {
}
