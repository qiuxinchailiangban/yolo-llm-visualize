package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.QrCodeView;
import com.hospital.followup.service.QrCodeService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/qrcodes")
public class AdminQrCodeController {

    private final QrCodeService qrCodeService;

    public AdminQrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/{id}")
    public ApiResponse<QrCodeView> getQrCode(@PathVariable Long id) {
        return ApiResponse.ok(qrCodeService.getQrCode(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable Long id) {
        byte[] imageBytes = qrCodeService.generateQrImage(id);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("qrcode-" + id + ".png").build().toString())
            .body(imageBytes);
    }
}
