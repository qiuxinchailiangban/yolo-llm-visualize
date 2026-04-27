package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientProcessExceptionItemView(
    String patientId,
    String patientName,
    String templateName,
    String instanceNo,
    String stepCode,
    String stepName,
    String exceptionType,
    String severity,
    String status,
    String reason,
    LocalDate surgeryDate,
    String surgeryScheduleTag,
    LocalDate plannedDate,
    LocalDateTime triggeredAt,
    String linkedQuestionnaireTaskNo,
    String linkedAutomationJobNo,
    LocalDateTime updatedAt
) {
}
