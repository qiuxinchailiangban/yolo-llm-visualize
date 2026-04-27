package com.hospital.followup.dto.admin;

import java.util.List;

public record DailyBatchReminderResult(
    int total,
    int queued,
    int skipped,
    int failed,
    List<Item> items
) {
    public record Item(
        String taskNo,
        String patientId,
        String patientName,
        String stageName,
        String dueDate,
        String targetConversation,
        String status, // QUEUED / SKIPPED_RECENT_REMINDER / SKIPPED_NO_CONTACT / FAILED
        String message
    ) {}
}
