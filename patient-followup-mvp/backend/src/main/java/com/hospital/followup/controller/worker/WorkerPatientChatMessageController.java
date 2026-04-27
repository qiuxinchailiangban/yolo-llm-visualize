package com.hospital.followup.controller.worker;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.worker.WorkerPatientChatMessageReportRequest;
import com.hospital.followup.dto.worker.WorkerPatientChatMessageReportView;
import com.hospital.followup.service.PatientChatMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker/patient-chat-messages")
public class WorkerPatientChatMessageController {

    private final PatientChatMessageService patientChatMessageService;

    public WorkerPatientChatMessageController(PatientChatMessageService patientChatMessageService) {
        this.patientChatMessageService = patientChatMessageService;
    }

    @PostMapping("/report")
    public ApiResponse<WorkerPatientChatMessageReportView> report(@Valid @RequestBody WorkerPatientChatMessageReportRequest request) {
        return ApiResponse.ok(patientChatMessageService.reportMessage(request), "患者群聊消息已处理");
    }
}
