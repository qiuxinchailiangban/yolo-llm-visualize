package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientProcessStepView(
    Long id,
    String stepCode,
    String stepName,
    Integer sortOrder,
    String stepType,
    String triggerMode,
    Integer relativeDayOffset,
    String relativeBase,
    String status,
    String statusReason,
    String feedbackSummary,
    Boolean feedbackRequired,
    LocalDate plannedDate,
    LocalDateTime plannedAt,
    LocalDateTime triggeredAt,
    LocalDateTime completedAt,
    String linkedQuestionnaireTaskNo,
    String linkedQuestionnaireStatus,
    java.time.LocalDate linkedQuestionnaireDueDate,
    java.time.LocalDateTime linkedQuestionnaireFinishedAt,
    java.time.LocalDateTime linkedQuestionnaireResponseSubmittedAt,
    String linkedQuestionnaireResponsePreview,
    String linkedAutomationJobNo,
    String linkedAutomationJobStatus,
    String linkedAutomationJobLastError,
    String linkedAutomationJobExecutionLog,
    String linkedMessageRuleCode,
    String applicableSurgeryTags,
    String displayHint
) {
}
