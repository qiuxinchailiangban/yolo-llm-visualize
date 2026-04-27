package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record PatientProcessTemplateView(
    Long id,
    String templateCode,
    String templateName,
    String templateCategory,
    String description,
    Boolean active,
    Boolean defaultTemplate,
    Boolean builtIn,
    Integer stepCount,
    LocalDateTime updatedAt,
    List<PatientProcessTemplateStepView> steps
) {
}
