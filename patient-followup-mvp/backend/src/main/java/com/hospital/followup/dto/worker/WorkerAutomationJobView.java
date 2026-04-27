package com.hospital.followup.dto.worker;

import java.time.LocalDateTime;

public record WorkerAutomationJobView(
    String jobNo,
    String jobType,
    String channel,
    String payloadJson,
    LocalDateTime plannedAt,
    Integer retryCount
) {
}
