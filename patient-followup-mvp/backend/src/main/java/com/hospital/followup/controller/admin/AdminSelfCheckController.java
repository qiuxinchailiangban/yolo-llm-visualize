package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.SelfCheckView;
import com.hospital.followup.service.SelfCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/self-check")
public class AdminSelfCheckController {

    private final SelfCheckService selfCheckService;

    public AdminSelfCheckController(SelfCheckService selfCheckService) {
        this.selfCheckService = selfCheckService;
    }

    @GetMapping
    public ApiResponse<SelfCheckView> runSelfCheck() {
        return ApiResponse.ok(selfCheckService.runFullCheck());
    }
}
