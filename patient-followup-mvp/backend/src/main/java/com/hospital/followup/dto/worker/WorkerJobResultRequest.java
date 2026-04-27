package com.hospital.followup.dto.worker;

import jakarta.validation.constraints.NotBlank;

public record WorkerJobResultRequest(
    @NotBlank(message = "workerId 不能为空") String workerId,
    String commandLine,
    String executionLog,
    String resultJson,
    String errorMessage
) {
}
