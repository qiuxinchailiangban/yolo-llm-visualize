package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientTaskView(
    String taskNo,
    String stageCode,
    String stageName,
    String templateName,
    String status,
    LocalDate dueDate,
    LocalDateTime finishedAt
) {
}
