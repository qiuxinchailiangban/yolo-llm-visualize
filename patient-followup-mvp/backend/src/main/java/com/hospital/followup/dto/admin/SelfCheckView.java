package com.hospital.followup.dto.admin;

import java.util.List;

public record SelfCheckView(
    boolean overallOk,
    List<SelfCheckItem> items
) {
    public record SelfCheckItem(
        String key,
        String name,
        String level,
        boolean ok,
        String message,
        String hint
    ) {}
}
