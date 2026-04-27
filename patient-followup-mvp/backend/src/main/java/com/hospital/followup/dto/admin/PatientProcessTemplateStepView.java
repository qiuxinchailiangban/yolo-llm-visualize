package com.hospital.followup.dto.admin;

public record PatientProcessTemplateStepView(
    Long id,
    String stepCode,
    String stepName,
    Integer sortOrder,
    String stepType,
    String triggerMode,
    Integer relativeDayOffset,
    String relativeBase,
    String description,
    String messageRuleCode,
    String stageCode,
    String templateCode,
    String completionRule,
    String applicableSurgeryTags,
    Boolean feedbackRequired,
    Boolean enabled
) {
}
