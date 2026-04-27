package com.hospital.followup.dto.worker;

public record WorkerPatientChatMessageReportView(
    boolean matched,
    String patientId,
    String patientName,
    String message
) {
}
