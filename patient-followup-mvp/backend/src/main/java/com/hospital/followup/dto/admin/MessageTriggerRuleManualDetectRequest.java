package com.hospital.followup.dto.admin;

import java.util.List;

public record MessageTriggerRuleManualDetectRequest(
    List<Long> ruleIds,
    List<String> patientIds
) {
}
