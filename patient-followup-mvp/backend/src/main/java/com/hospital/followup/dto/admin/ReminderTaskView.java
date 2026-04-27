package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record ReminderTaskView(
    Long id,
    String taskNo,
    String targetConversation,
    String contentPreview,
    String reminderChannel,
    String status,
    String failReason,
    LocalDateTime plannedAt,
    LocalDateTime startedAt,
    LocalDateTime sentAt,
    LocalDateTime finishedAt,
    String commandLine,
    String executionLog
) {
}
