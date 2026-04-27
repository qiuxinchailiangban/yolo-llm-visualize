package com.hospital.followup.dto.admin;

import java.util.List;

public record MessageTriggerRuleManualExecuteResult(
    int total,
    int queued,
    int skipped,
    List<MessageTriggerRuleManualExecuteItemView> items
) {
}
