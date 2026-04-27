package com.hospital.followup.dto.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkerWechatGroupDiscoveryView(
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
    LocalDateTime discoveredAt,
    LocalDateTime lastSeenAt
) {
}
