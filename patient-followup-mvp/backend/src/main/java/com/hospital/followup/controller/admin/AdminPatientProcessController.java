package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.PatientProcessDashboardView;
import com.hospital.followup.dto.admin.PatientProcessDetailView;
import com.hospital.followup.dto.admin.PatientProcessExceptionCenterView;
import com.hospital.followup.dto.admin.PatientProcessTemplateUpsertRequest;
import com.hospital.followup.dto.admin.PatientProcessTemplateView;
import com.hospital.followup.service.PatientProcessService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/patient-processes")
public class AdminPatientProcessController {

    private final PatientProcessService patientProcessService;

    public AdminPatientProcessController(PatientProcessService patientProcessService) {
        this.patientProcessService = patientProcessService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<PatientProcessDashboardView> dashboard(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(patientProcessService.getDashboard(keyword));
    }

    @GetMapping("/templates")
    public ApiResponse<List<PatientProcessTemplateView>> listTemplates() {
        return ApiResponse.ok(patientProcessService.listTemplates());
    }

    @GetMapping("/exceptions")
    public ApiResponse<PatientProcessExceptionCenterView> exceptions() {
        return ApiResponse.ok(patientProcessService.getExceptionCenter());
    }

    @PostMapping("/templates")
    public ApiResponse<PatientProcessTemplateView> createTemplate(@Valid @RequestBody PatientProcessTemplateUpsertRequest request) {
        return ApiResponse.ok(patientProcessService.createTemplate(request), "流程模板创建成功");
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<PatientProcessTemplateView> updateTemplate(
        @PathVariable Long id,
        @Valid @RequestBody PatientProcessTemplateUpsertRequest request
    ) {
        return ApiResponse.ok(patientProcessService.updateTemplate(id, request), "流程模板更新成功");
    }

    @GetMapping("/{patientId}")
    public ApiResponse<PatientProcessDetailView> detail(@PathVariable String patientId) {
        return ApiResponse.ok(patientProcessService.getPatientProcessDetail(patientId));
    }

    @PostMapping("/{patientId}/sync")
    public ApiResponse<PatientProcessDetailView> sync(@PathVariable String patientId) {
        return ApiResponse.ok(patientProcessService.syncAndGetPatientProcessDetail(patientId), "患者流程已同步");
    }
}
