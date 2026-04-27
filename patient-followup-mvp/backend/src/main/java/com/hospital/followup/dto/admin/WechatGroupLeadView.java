package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WechatGroupLeadView(
    String chatroomUsername,
    String chatroomDisplayName,
    String rawGroupName,
    String parseStatus,
    String parseMessage,
    String groupStage,
    String eventDateText,
    LocalDate eventDate,
    String assistantDoctorName,
    String patientName,
    String surgerySite,
    String surgeryType,
    String linkedPatientId,
    String linkedPatientName,
    String reporterWorkerId,
    String sourceChannel,
    String firstMessageSnippet,
    String lastMessageSnippet,
    LocalDateTime discoveredAt,
    LocalDateTime lastSeenAt,
    LocalDateTime updatedAt
) {
}
