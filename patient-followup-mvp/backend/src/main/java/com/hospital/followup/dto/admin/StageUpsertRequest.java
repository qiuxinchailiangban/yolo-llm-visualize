package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StageUpsertRequest(
    @NotBlank(message = "阶段编码不能为空") String stageCode,
    @NotBlank(message = "阶段名称不能为空") String stageName,
    @NotNull(message = "天数偏移不能为空") Integer dayOffset,
    @NotNull(message = "排序不能为空") Integer sortOrder,
    @NotNull(message = "启用状态不能为空") Boolean enabled,
    @NotNull(message = "提醒开关不能为空") Boolean reminderEnabled,
    String description
) {
}
