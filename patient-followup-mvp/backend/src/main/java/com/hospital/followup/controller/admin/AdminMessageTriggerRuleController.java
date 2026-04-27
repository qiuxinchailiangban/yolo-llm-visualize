package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualCandidateView;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualDetectRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteResult;
import com.hospital.followup.dto.admin.MessageTriggerRuleMediaUploadView;
import com.hospital.followup.dto.admin.MessageTriggerRuleUpsertRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleView;
import com.hospital.followup.service.MessageTriggerRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/message-trigger-rules")
public class AdminMessageTriggerRuleController {

    private final MessageTriggerRuleService messageTriggerRuleService;

    public AdminMessageTriggerRuleController(MessageTriggerRuleService messageTriggerRuleService) {
        this.messageTriggerRuleService = messageTriggerRuleService;
    }

    @GetMapping
    public ApiResponse<List<MessageTriggerRuleView>> listRules() {
        return ApiResponse.ok(messageTriggerRuleService.listRules());
    }

    @GetMapping("/manual-rules")
    public ApiResponse<List<MessageTriggerRuleView>> listManualRules() {
        return ApiResponse.ok(messageTriggerRuleService.listManualRules());
    }

    @PostMapping
    public ApiResponse<MessageTriggerRuleView> createRule(@Valid @org.springframework.web.bind.annotation.RequestBody MessageTriggerRuleUpsertRequest request) {
        return ApiResponse.ok(messageTriggerRuleService.createRule(request), "任务定义创建成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<MessageTriggerRuleView> updateRule(
        @PathVariable Long id,
        @Valid @org.springframework.web.bind.annotation.RequestBody MessageTriggerRuleUpsertRequest request
    ) {
        return ApiResponse.ok(messageTriggerRuleService.updateRule(id, request), "任务定义更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        messageTriggerRuleService.deleteRule(id);
        return ApiResponse.ok(null, "任务定义已删除");
    }

    @PostMapping("/upload-media")
    public ApiResponse<MessageTriggerRuleMediaUploadView> uploadMedia(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(messageTriggerRuleService.uploadMedia(file), "图片上传成功");
    }

    @PostMapping("/manual-detect")
    public ApiResponse<List<MessageTriggerRuleManualCandidateView>> detectManualCandidates(
        @org.springframework.web.bind.annotation.RequestBody(required = false) MessageTriggerRuleManualDetectRequest request
    ) {
        MessageTriggerRuleManualDetectRequest effective = request == null
            ? new MessageTriggerRuleManualDetectRequest(List.of(), List.of())
            : request;
        return ApiResponse.ok(messageTriggerRuleService.detectManualCandidates(effective));
    }

    @PostMapping("/manual-execute")
    public ApiResponse<MessageTriggerRuleManualExecuteResult> executeManualCandidates(
        @Valid @org.springframework.web.bind.annotation.RequestBody MessageTriggerRuleManualExecuteRequest request
    ) {
        return ApiResponse.ok(messageTriggerRuleService.executeManualCandidates(request), "手动任务已加入队列");
    }
}
