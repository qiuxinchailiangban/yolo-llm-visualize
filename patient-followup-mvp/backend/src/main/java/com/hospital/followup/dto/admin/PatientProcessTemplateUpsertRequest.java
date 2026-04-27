package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientProcessTemplateUpsertRequest(
    String templateCode,
    String templateName,
    String templateCategory,
    String description,
    Boolean active,
    Boolean defaultTemplate,
    List<PatientProcessTemplateStepRequest> steps
) {
}
