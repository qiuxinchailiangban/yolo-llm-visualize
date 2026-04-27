package com.hospital.followup.dto.admin;

public record StageView(
    Long id,
    String stageCode,
    String stageName,
    Integer dayOffset,
    Integer sortOrder,
    Boolean enabled,
    Boolean reminderEnabled,
    String description
) {
}
