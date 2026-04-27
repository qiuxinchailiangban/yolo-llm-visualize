package com.hospital.followup.dto.worker;

import com.hospital.followup.domain.enums.AutomationJobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkerClaimJobRequest(
    @NotBlank(message = "workerId 不能为空") String workerId,
    @NotNull(message = "jobType 不能为空") AutomationJobType jobType
) {
}
