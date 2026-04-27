package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientProcessOverviewView(
    String instanceNo,
    String patientId,
    String patientName,
    String diagnosis,
    LocalDate surgeryDate,
    String templateName,
    String status,
    String currentStepCode,
    String currentStepName,
    Integer totalStepCount,
    Integer completedStepCount,
    Integer waitingFeedbackCount,
    Integer warningStepCount,
    Integer progressPercent,
    String summaryText,
    LocalDateTime updatedAt
) {
}
