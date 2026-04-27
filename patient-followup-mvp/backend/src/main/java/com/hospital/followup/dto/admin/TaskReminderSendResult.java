package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record TaskReminderSendResult(
    Long reminderTaskId,
    String taskNo,
    String patientId,
    String patientName,
    String targetConversation,
    String reminderChannel,
    String status,
    String message,
    Integer countdownSeconds,
    LocalDateTime startedAt,
    LocalDateTime sentAt,
    LocalDateTime finishedAt,
    String output,
    String commandLine
) {
}
