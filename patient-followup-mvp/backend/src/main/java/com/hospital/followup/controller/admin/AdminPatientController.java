package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.domain.enums.PatientStatus;
import com.hospital.followup.dto.admin.CreatePatientRequest;
import com.hospital.followup.dto.admin.PatientImportRequest;
import com.hospital.followup.dto.admin.PatientImportResult;
import com.hospital.followup.dto.admin.PatientDetailView;
import com.hospital.followup.dto.admin.PatientTaskRebuildResult;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.service.PatientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final PatientService patientService;

    public AdminPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ApiResponse<List<PatientView>> listPatients(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) PatientStatus status
    ) {
        return ApiResponse.ok(patientService.listPatients(keyword, status));
    }

    @GetMapping("/{patientId}")
    public ApiResponse<PatientDetailView> getPatient(@PathVariable String patientId) {
        return ApiResponse.ok(patientService.getPatientDetail(patientId));
    }

    @PostMapping
    public ApiResponse<PatientView> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.ok(patientService.createPatient(request), "患者创建成功");
    }

    @PutMapping("/{patientId}")
    public ApiResponse<PatientView> updatePatient(@PathVariable String patientId, @Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.ok(patientService.updatePatient(patientId, request), "患者信息更新成功");
    }

    @DeleteMapping("/{patientId}")
    public ApiResponse<Void> deletePatient(@PathVariable String patientId) {
        patientService.deletePatient(patientId);
        return ApiResponse.ok(null, "患者删除成功");
    }

    @PostMapping("/rebuild-tasks")
    public ApiResponse<PatientTaskRebuildResult> rebuildAllPatientTasks() {
        return ApiResponse.ok(patientService.rebuildAllPatientTasks(), "已完成所有患者任务重建");
    }

    @PostMapping("/import-csv")
    public ApiResponse<PatientImportResult> importPatients(@Valid @RequestBody PatientImportRequest request) {
        return ApiResponse.ok(patientService.importPatientsFromCsv(request.csvContent()), "CSV 导入完成");
    }
}
