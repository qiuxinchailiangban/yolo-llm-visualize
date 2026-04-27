package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record PatientChatMessageView(
    Long id,
    String chatroomName,
    String senderDisplayName,
    String senderUsername,
    String direction,
    String messageType,
    String contentPreview,
    String content,
    LocalDateTime messageTime
) {
}
