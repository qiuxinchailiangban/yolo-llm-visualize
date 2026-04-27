package com.hospital.followup.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageTriggerRuleScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageTriggerRuleScheduler.class);

    private final MessageTriggerRuleService messageTriggerRuleService;

    public MessageTriggerRuleScheduler(MessageTriggerRuleService messageTriggerRuleService) {
        this.messageTriggerRuleService = messageTriggerRuleService;
    }

    @Scheduled(
        fixedDelayString = "${app.message-trigger.fixed-delay-ms:60000}",
        initialDelayString = "${app.message-trigger.initial-delay-ms:15000}"
    )
    public void scanAndQueueDueRules() {
        try {
            messageTriggerRuleService.scanAndQueueDueRules();
        } catch (Exception error) {
            log.warn("[message-trigger-rule] 定时扫描失败: {}", error.getMessage(), error);
        }
    }
}
