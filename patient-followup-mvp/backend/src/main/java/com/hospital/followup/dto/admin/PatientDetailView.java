package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientDetailView(
    PatientView patient,
    List<PatientTaskView> tasks,
    List<PatientChatMessageView> recentChatMessages
) {
}
