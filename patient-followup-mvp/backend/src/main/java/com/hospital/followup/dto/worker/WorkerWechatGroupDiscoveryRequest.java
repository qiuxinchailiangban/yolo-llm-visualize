package com.hospital.followup.dto.worker;

import jakarta.validation.constraints.NotBlank;

public record WorkerWechatGroupDiscoveryRequest(
    @NotBlank(message = "workerId 不能为空") String workerId,
    @NotBlank(message = "chatroomUsername 不能为空") String chatroomUsername,
    String chatroomDisplayName,
    String rawGroupName,
    String firstMessageSnippet,
    String lastMessageSnippet
) {
}
