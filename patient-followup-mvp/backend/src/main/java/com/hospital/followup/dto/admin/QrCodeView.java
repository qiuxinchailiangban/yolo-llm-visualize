package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record QrCodeView(
    Long id,
    String qrType,
    String targetCode,
    String targetName,
    String token,
    String status,
    String pagePath,
    String imageMode,
    boolean wechatConfigured,
    String debugUrl,
    LocalDateTime expiresAt
) {
}
