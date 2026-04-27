package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.DashboardView;
import com.hospital.followup.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/todos")
    public ApiResponse<DashboardView> getDashboardTodos() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
