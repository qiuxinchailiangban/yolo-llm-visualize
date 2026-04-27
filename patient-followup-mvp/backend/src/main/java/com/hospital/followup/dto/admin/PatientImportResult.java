package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientImportResult(
    int totalRows,
    int successRows,
    int createdCount,
    int updatedCount,
    int skippedCount,
    int totalTasksGenerated,
    List<PatientImportRowResult> rows
) {
}
