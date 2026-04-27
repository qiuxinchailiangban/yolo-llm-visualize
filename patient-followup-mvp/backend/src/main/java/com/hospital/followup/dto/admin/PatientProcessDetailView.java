package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record PatientProcessDetailView(
    String instanceNo,
    PatientView patient,
    String templateCode,
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
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    List<PatientProcessStepView> steps
) {
}
