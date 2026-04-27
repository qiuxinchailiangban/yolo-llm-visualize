package com.hospital.followup.dto.publicapi;

public record FollowUpSharedSubmissionResult(
    String taskNo,
    String patientName,
    String stageName
) {
}
