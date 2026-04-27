package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.WechatGroupLeadBindPatientRequest;
import com.hospital.followup.dto.admin.WechatGroupLeadView;
import com.hospital.followup.service.WechatGroupLeadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wechat-group-leads")
public class AdminWechatGroupLeadController {

    private final WechatGroupLeadService wechatGroupLeadService;

    public AdminWechatGroupLeadController(WechatGroupLeadService wechatGroupLeadService) {
        this.wechatGroupLeadService = wechatGroupLeadService;
    }

    @GetMapping
    public ApiResponse<List<WechatGroupLeadView>> list() {
        return ApiResponse.ok(wechatGroupLeadService.listLeads());
    }

    @PostMapping("/{chatroomUsername}/create-patient")
    public ApiResponse<WechatGroupLeadView> createPatient(@PathVariable String chatroomUsername) {
        return ApiResponse.ok(wechatGroupLeadService.createPatientFromLead(chatroomUsername), "已从微信群线索创建患者");
    }

    @PostMapping("/{chatroomUsername}/bind-patient")
    public ApiResponse<WechatGroupLeadView> bindPatient(
        @PathVariable String chatroomUsername,
        @Valid @RequestBody WechatGroupLeadBindPatientRequest request
    ) {
        return ApiResponse.ok(wechatGroupLeadService.bindExistingPatient(chatroomUsername, request), "已绑定患者");
    }

    @DeleteMapping("/{chatroomUsername}/bind-patient")
    public ApiResponse<WechatGroupLeadView> unbindPatient(@PathVariable String chatroomUsername) {
        return ApiResponse.ok(wechatGroupLeadService.unbindPatient(chatroomUsername), "已解除患者绑定");
    }
}
