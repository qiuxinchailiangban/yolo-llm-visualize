package com.hospital.followup.dto.admin;

public record PatientImportRowResult(
    int rowNumber,
    String patientName,
    String phone,
    String surgeryDate,
    String action,
    String patientId,
    int taskCount,
    String message
) {
}
