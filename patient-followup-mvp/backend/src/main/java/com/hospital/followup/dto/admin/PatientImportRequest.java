package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record PatientImportRequest(
    @NotBlank(message = "不能为空") String csvContent
) {
}
