package com.hospital.followup.controller.worker;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.worker.WorkerAutomationJobView;
import com.hospital.followup.dto.worker.WorkerClaimJobRequest;
import com.hospital.followup.dto.worker.WorkerJobResultRequest;
import com.hospital.followup.service.AutomationJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker/automation-jobs")
public class WorkerAutomationJobController {

    private final AutomationJobService automationJobService;

    public WorkerAutomationJobController(AutomationJobService automationJobService) {
        this.automationJobService = automationJobService;
    }

    @PostMapping("/claim")
    public ApiResponse<WorkerAutomationJobView> claimJob(@Valid @RequestBody WorkerClaimJobRequest request) {
        return ApiResponse.ok(automationJobService.claimNextJob(request));
    }

    @PostMapping("/{jobNo}/complete")
    public ApiResponse<Void> completeJob(@PathVariable String jobNo, @Valid @RequestBody WorkerJobResultRequest request) {
        automationJobService.completeJob(jobNo, request);
        return ApiResponse.ok(null, "任务完成");
    }

    @PostMapping("/{jobNo}/fail")
    public ApiResponse<Void> failJob(@PathVariable String jobNo, @Valid @RequestBody WorkerJobResultRequest request) {
        automationJobService.failJob(jobNo, request);
        return ApiResponse.ok(null, "任务失败已回写");
    }
}
