package com.hospital.followup.controller.publicapi;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.publicapi.FollowUpSharedSubmissionRequest;
import com.hospital.followup.dto.publicapi.FollowUpSharedSubmissionResult;
import com.hospital.followup.dto.publicapi.IntakeSubmissionRequest;
import com.hospital.followup.dto.publicapi.IntakeSubmissionResult;
import com.hospital.followup.dto.publicapi.PublicQrCodeResolveView;
import com.hospital.followup.dto.publicapi.PublicTaskDetailView;
import com.hospital.followup.dto.publicapi.PublicTemplatePayload;
import com.hospital.followup.dto.publicapi.TaskSubmissionRequest;
import com.hospital.followup.service.PatientService;
import com.hospital.followup.service.QrCodeService;
import com.hospital.followup.service.QuestionnaireService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicQuestionnaireController {

    private final QuestionnaireService questionnaireService;
    private final PatientService patientService;
    private final QrCodeService qrCodeService;

    public PublicQuestionnaireController(
        QuestionnaireService questionnaireService,
        PatientService patientService,
        QrCodeService qrCodeService
    ) {
        this.questionnaireService = questionnaireService;
        this.patientService = patientService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/intake-template")
    public ApiResponse<PublicTemplatePayload> getIntakeTemplate() {
        return ApiResponse.ok(questionnaireService.getIntakeTemplate());
    }

    @PostMapping("/intake-submissions")
    public ApiResponse<IntakeSubmissionResult> submitIntake(@Valid @RequestBody IntakeSubmissionRequest request) {
        return ApiResponse.ok(patientService.submitIntake(request), "首诊信息提交成功");
    }

    @GetMapping("/tasks/{taskNo}")
    public ApiResponse<PublicTaskDetailView> getTaskDetail(@PathVariable String taskNo) {
        questionnaireService.refreshOverdueTasks();
        return ApiResponse.ok(questionnaireService.getTaskDetail(taskNo));
    }

    @GetMapping("/qrcode/resolve")
    public ApiResponse<PublicQrCodeResolveView> resolveQrCode(@org.springframework.web.bind.annotation.RequestParam String token) {
        return ApiResponse.ok(qrCodeService.resolveForMiniapp(token));
    }

    @PostMapping("/tasks/{taskNo}/submit")
    public ApiResponse<Void> submitTask(@PathVariable String taskNo, @Valid @RequestBody TaskSubmissionRequest request) {
        questionnaireService.submitTask(taskNo, request);
        return ApiResponse.ok(null, "问卷提交成功");
    }

    /**
     * 共享随访模板二维码的提交入口：患者扫码后填姓名+手机+答案，
     * 后端按手机+姓名匹配患者并定位 ta 在该阶段的待填任务。
     */
    @PostMapping("/follow-up-submissions")
    public ApiResponse<FollowUpSharedSubmissionResult> submitFollowUpShared(
        @Valid @RequestBody FollowUpSharedSubmissionRequest request
    ) {
        return ApiResponse.ok(qrCodeService.submitFollowUpShared(request), "问卷提交成功");
    }
}
