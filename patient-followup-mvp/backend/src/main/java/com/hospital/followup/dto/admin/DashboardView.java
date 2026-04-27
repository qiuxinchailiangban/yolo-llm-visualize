package com.hospital.followup.dto.admin;

import java.util.List;

public record DashboardView(
    long surgeryTodayCount,
    long questionnaireDueTodayCount,
    long remindableCount,
    List<DashboardTodoItem> surgeriesToday,
    List<DashboardTodoItem> questionnaireDueToday,
    List<DashboardTodoItem> remindablePatients,
    List<DashboardTodoItem> allPatients
) {
}
