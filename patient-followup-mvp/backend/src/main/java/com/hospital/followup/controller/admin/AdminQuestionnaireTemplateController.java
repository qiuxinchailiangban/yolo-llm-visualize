package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.dto.admin.QrCodeCreateRequest;
import com.hospital.followup.dto.admin.QrCodeView;
import com.hospital.followup.dto.admin.TemplateUpsertRequest;
import com.hospital.followup.dto.admin.TemplateView;
import com.hospital.followup.service.QrCodeService;
import com.hospital.followup.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminQuestionnaireTemplateController {

    private final TemplateService templateService;
    private final QrCodeService qrCodeService;

    public AdminQuestionnaireTemplateController(TemplateService templateService, QrCodeService qrCodeService) {
        this.templateService = templateService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/templates")
    public ApiResponse<List<TemplateView>> listTemplates(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) TemplateStatus status
    ) {
        return ApiResponse.ok(templateService.listTemplates(keyword, status));
    }

    @PostMapping("/templates")
    public ApiResponse<TemplateView> createTemplate(@Valid @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(templateService.createTemplate(request), "模板创建成功");
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<TemplateView> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(templateService.updateTemplate(id, request), "模板更新成功");
    }

    @PostMapping("/templates/{id}/qrcodes")
    public ApiResponse<QrCodeView> createTemplateQrCode(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) QrCodeCreateRequest request
    ) {
        return ApiResponse.ok(qrCodeService.createTemplateQrCode(id, request), "模板二维码生成成功");
    }
}
