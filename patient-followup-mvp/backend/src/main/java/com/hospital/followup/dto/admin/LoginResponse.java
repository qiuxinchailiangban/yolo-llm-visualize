package com.hospital.followup.dto.admin;

import java.time.LocalDateTime;

public record LoginResponse(
    String token,
    LocalDateTime expiresAt,
    AdminUserView user
) {
}
