package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MessageTriggerRuleManualExecuteRequest(
    @NotNull(message = "执行项不能为空") List<MessageTriggerRuleManualExecuteItemRequest> items
) {
}
