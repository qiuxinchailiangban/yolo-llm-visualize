package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DailyBatchReminderRequest(
    @Min(value = 0, message = "倒计时秒数必须 >= 0")
    @Max(value = 300, message = "倒计时秒数不能超过 300")
    Integer countdownSeconds,
    Boolean skipRecentlyReminded,
    @Min(value = 1, message = "去重窗口必须 >= 1 小时")
    @Max(value = 72, message = "去重窗口不能超过 72 小时")
    Integer recentWindowHours,
    String contentTemplate
) {
    public int resolvedCountdownSeconds() {
        return countdownSeconds == null ? 10 : countdownSeconds;
    }

    public boolean resolvedSkipRecentlyReminded() {
        return skipRecentlyReminded == null || skipRecentlyReminded;
    }

    public int resolvedRecentWindowHours() {
        return recentWindowHours == null ? 2 : recentWindowHours;
    }
}
