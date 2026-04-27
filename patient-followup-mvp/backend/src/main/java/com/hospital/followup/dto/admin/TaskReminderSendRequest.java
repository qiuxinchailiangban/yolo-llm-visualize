package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TaskReminderSendRequest(
    @NotBlank(message = "目标会话不能为空") String targetConversation,
    @NotBlank(message = "提醒内容不能为空") String content,
    @Positive(message = "倒计时秒数必须大于 0") Integer countdownSeconds
) {
}
