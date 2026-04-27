package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientView(
    String patientId,
    String name,
    String gender,
    String phone,
    LocalDate birthDate,
    LocalDate surgeryDate,
    String surgeryScheduleTag,
    String surgeryTimeText,
    String diagnosis,
    String sourceChannel,
    String wechatChatroomUsername,
    String wechatChatroomDisplayName,
    String wechatGroupName,
    String status,
    LocalDateTime createdAt
) {
}
