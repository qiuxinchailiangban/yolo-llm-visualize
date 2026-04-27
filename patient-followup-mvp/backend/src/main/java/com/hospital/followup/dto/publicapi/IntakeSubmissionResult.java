package com.hospital.followup.dto.publicapi;

import java.util.List;

public record IntakeSubmissionResult(
    String patientId,
    List<String> createdTaskNos,
    String message
) {
}
