package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePatientRequest(
    @NotBlank(message = "不能为空") String name,
    String gender,
    String phone,
    LocalDate birthDate,
    LocalDate surgeryDate,
    String surgeryScheduleTag,
    String surgeryTimeText,
    String diagnosis,
    String sourceChannel
) {
}
