package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record QrCodeCreateRequest(
    @Min(1) @Max(365) Integer expireDays
) {
}
