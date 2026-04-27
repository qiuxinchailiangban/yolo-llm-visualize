package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record AutomationJobView(
    Long id,
    String jobNo,
    String jobType,
    String bizType,
    Long bizId,
    String channel,
    String status,
    LocalDateTime plannedAt,
    LocalDateTime claimedAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String workerId,
    Integer retryCount,
    String lastError,
    String executionLog
) {
}
