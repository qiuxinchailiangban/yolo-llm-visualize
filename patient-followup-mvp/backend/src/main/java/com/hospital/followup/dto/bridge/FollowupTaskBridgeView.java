package com.hospital.followup.dto.bridge;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowupTaskBridgeView(
    PatientPayload patient,
    TaskPayload task,
    StagePayload stage,
    TemplatePayload template,
    ResponsePayload response
) {

    public record PatientPayload(
        String patientId,
        String patientNo,
        String name,
        String gender,
        String phone,
        LocalDate birthDate,
        LocalDate surgeryDate,
        String diagnosis,
        String sourceChannel,
        String status
    ) {
    }

    public record TaskPayload(
        String taskNo,
        String status,
        LocalDate dueDate,
        LocalDateTime finishedAt
    ) {
    }

    public record StagePayload(
        Long id,
        String stageCode,
        String stageName,
        Integer dayOffset,
        Boolean reminderEnabled
    ) {
    }

    public record TemplatePayload(
        Long id,
        String templateCode,
        String templateName,
        String templateType,
        String version,
        String status,
        String schemaJson
    ) {
    }

    public record ResponsePayload(
        Long id,
        String answersJson,
        String submitChannel,
        LocalDateTime submittedAt
    ) {
    }
}
