package com.hospital.followup.controller.worker;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.worker.WorkerWechatGroupDiscoveryRequest;
import com.hospital.followup.dto.worker.WorkerWechatGroupDiscoveryView;
import com.hospital.followup.service.WechatGroupLeadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker/wechat-group-leads")
public class WorkerWechatGroupLeadController {

    private final WechatGroupLeadService wechatGroupLeadService;

    public WorkerWechatGroupLeadController(WechatGroupLeadService wechatGroupLeadService) {
        this.wechatGroupLeadService = wechatGroupLeadService;
    }

    @PostMapping("/discover")
    public ApiResponse<WorkerWechatGroupDiscoveryView> discover(@Valid @RequestBody WorkerWechatGroupDiscoveryRequest request) {
        return ApiResponse.ok(wechatGroupLeadService.registerDiscovery(request), "微信群线索已记录");
    }
}
