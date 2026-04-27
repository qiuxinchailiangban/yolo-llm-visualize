package com.hospital.followup.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkerPatientChatMessageReportRequest(
    @NotBlank(message = "workerId 不能为空") String workerId,
    @NotBlank(message = "chatroomUsername 不能为空") String chatroomUsername,
    String chatroomDisplayName,
    String chatroomName,
    String senderDisplayName,
    String senderUsername,
    @NotBlank(message = "direction 不能为空") String direction,
    @NotBlank(message = "messageType 不能为空") String messageType,
    @NotBlank(message = "content 不能为空") String content,
    Long localMessageId,
    Long serverMessageId,
    @NotNull(message = "messageEpochSeconds 不能为空") Long messageEpochSeconds
) {
}
