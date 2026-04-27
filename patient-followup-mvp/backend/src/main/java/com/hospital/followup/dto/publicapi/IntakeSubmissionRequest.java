package com.hospital.followup.dto.publicapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public record IntakeSubmissionRequest(
    @NotBlank(message = "姓名不能为空") String name,
    String gender,
    String phone,
    LocalDate birthDate,
    @NotNull(message = "手术日期不能为空") LocalDate surgeryDate,
    String diagnosis,
    Map<String, Object> answers
) {
}
