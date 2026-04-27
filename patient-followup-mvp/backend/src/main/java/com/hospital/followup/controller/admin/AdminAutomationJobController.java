package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.AutomationJobView;
import com.hospital.followup.service.AutomationJobService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/automation-jobs")
public class AdminAutomationJobController {

    private final AutomationJobService automationJobService;

    public AdminAutomationJobController(AutomationJobService automationJobService) {
        this.automationJobService = automationJobService;
    }

    @GetMapping
    public ApiResponse<List<AutomationJobView>> listRecentJobs() {
        return ApiResponse.ok(automationJobService.listRecentJobs());
    }
}
