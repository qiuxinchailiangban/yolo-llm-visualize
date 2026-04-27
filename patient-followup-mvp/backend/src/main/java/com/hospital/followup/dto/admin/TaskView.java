package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskView(
    String taskNo,
    String patientId,
    String patientName,
    String preferredConversation,
    String phone,
    LocalDate surgeryDate,
    String stageCode,
    String stageName,
    String templateName,
    String status,
    LocalDate dueDate,
    LocalDateTime finishedAt
) {
}
