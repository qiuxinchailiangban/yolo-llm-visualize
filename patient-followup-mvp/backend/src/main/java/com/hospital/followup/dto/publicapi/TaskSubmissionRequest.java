package com.hospital.followup.dto.publicapi;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TaskSubmissionRequest(
    @NotNull(message = "answers不能为空") Map<String, Object> answers
) {
}
