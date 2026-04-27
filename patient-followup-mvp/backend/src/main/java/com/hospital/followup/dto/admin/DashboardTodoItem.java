package com.hospital.followup.dto.admin;

public record DashboardTodoItem(
    String patientId,
    String patientName,
    String taskNo,
    String stageName,
    String dueDate,
    String remark
) {
}
