package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record WechatGroupLeadBindPatientRequest(
    @NotBlank(message = "patientId 不能为空") String patientId
) {
}
