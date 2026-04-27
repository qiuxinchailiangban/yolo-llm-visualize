package com.hospital.followup.dto.admin;

public record PatientTaskRebuildResult(
    int totalPatients,
    int rebuiltPatients,
    int skippedPatients,
    int totalTasksAffected
) {
}
