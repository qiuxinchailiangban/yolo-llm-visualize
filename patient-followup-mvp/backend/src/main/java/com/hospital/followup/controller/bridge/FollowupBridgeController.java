package com.hospital.followup.controller.bridge;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.bridge.FollowupTaskBridgeView;
import com.hospital.followup.service.FollowupBridgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bridge")
public class FollowupBridgeController {

    private final FollowupBridgeService followupBridgeService;

    public FollowupBridgeController(FollowupBridgeService followupBridgeService) {
        this.followupBridgeService = followupBridgeService;
    }

    @GetMapping("/followup-tasks/{taskNo}")
    public ApiResponse<FollowupTaskBridgeView> getFollowupTask(@PathVariable String taskNo) {
        return ApiResponse.ok(followupBridgeService.getFollowupTask(taskNo));
    }
}
