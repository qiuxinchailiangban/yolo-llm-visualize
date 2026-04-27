package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientProcessDashboardView(
    Integer totalPatients,
    Integer activeInstances,
    Integer waitingFeedbackPatients,
    Integer warningPatients,
    List<PatientProcessOverviewView> items
) {
}
