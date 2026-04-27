package com.hospital.followup.dto.admin;

import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
    @NotNull(message = "状态不能为空") QuestionnaireTaskStatus status
) {
}
