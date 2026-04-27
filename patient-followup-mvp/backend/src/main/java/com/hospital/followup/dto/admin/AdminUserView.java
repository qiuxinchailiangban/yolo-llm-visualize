package com.hospital.followup.dto.admin;

public record AdminUserView(
    String username,
    String displayName,
    String role
) {
}
