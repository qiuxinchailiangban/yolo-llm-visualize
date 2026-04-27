package com.hospital.followup.dto.admin;

import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateUpsertRequest(
    @NotBlank(message = "不能为空") String templateCode,
    @NotBlank(message = "不能为空") String templateName,
    @NotNull(message = "不能为空") TemplateType templateType,
    @NotBlank(message = "不能为空") String version,
    Long stageId,
    @NotNull(message = "不能为空") TemplateStatus status,
    @NotBlank(message = "不能为空") String schemaJson,
    String description
) {
}
