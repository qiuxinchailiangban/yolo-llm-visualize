package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientProcessExceptionCenterView(
    Integer total,
    Integer warningCount,
    Integer feedbackTimeoutCount,
    Integer sendFailureCount,
    List<PatientProcessExceptionItemView> items
) {
}
