package com.hospital.followup.service;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.PatientChatMessage;
import com.hospital.followup.dto.admin.PatientChatMessageView;
import com.hospital.followup.dto.worker.WorkerPatientChatMessageReportRequest;
import com.hospital.followup.dto.worker.WorkerPatientChatMessageReportView;
import com.hospital.followup.repository.PatientChatMessageRepository;
import com.hospital.followup.repository.PatientRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PatientChatMessageService {

    private final PatientRepository patientRepository;
    private final PatientChatMessageRepository patientChatMessageRepository;
    private final MessageTriggerRuleService messageTriggerRuleService;

    public PatientChatMessageService(
        PatientRepository patientRepository,
        PatientChatMessageRepository patientChatMessageRepository,
        MessageTriggerRuleService messageTriggerRuleService
    ) {
        this.patientRepository = patientRepository;
        this.patientChatMessageRepository = patientChatMessageRepository;
        this.messageTriggerRuleService = messageTriggerRuleService;
    }

    @Transactional
    public WorkerPatientChatMessageReportView reportMessage(WorkerPatientChatMessageReportRequest request) {
        Patient patient = patientRepository.findByWechatChatroomUsername(request.chatroomUsername()).orElse(null);
        if (patient == null) {
            return new WorkerPatientChatMessageReportView(false, null, null, "未匹配到已绑定患者，已跳过");
        }

        String messageKey = buildMessageKey(request);
        if (patientChatMessageRepository.existsByMessageKey(messageKey)) {
            return new WorkerPatientChatMessageReportView(true, patient.getPatientId(), patient.getName(), "消息已存在，跳过重复写入");
        }

        PatientChatMessage message = new PatientChatMessage();
        message.setPatient(patient);
        message.setMessageKey(messageKey);
        message.setChatroomUsername(trimToEmpty(request.chatroomUsername()));
        message.setChatroomDisplayName(trimToNull(request.chatroomDisplayName()));
        message.setChatroomName(resolveChatroomName(request));
        message.setSenderDisplayName(trimToNull(request.senderDisplayName()));
        message.setSenderUsername(trimToNull(request.senderUsername()));
        message.setDirection(trimToEmpty(request.direction()));
        message.setMessageType(trimToEmpty(request.messageType()));
        message.setContent(trimToEmpty(request.content()));
        message.setContentPreview(truncate(trimToEmpty(request.content()), 255));
        message.setLocalMessageId(request.localMessageId());
        message.setServerMessageId(request.serverMessageId());
        message.setReporterWorkerId(trimToNull(request.workerId()));
        message.setMessageTime(LocalDateTime.ofInstant(
            Instant.ofEpochSecond(request.messageEpochSeconds()),
            ZoneId.systemDefault()
        ));
        patientChatMessageRepository.save(message);
        messageTriggerRuleService.queueKeywordRulesForMessage(patient, message);
        return new WorkerPatientChatMessageReportView(true, patient.getPatientId(), patient.getName(), "已归属到患者并记录消息");
    }

    @Transactional(readOnly = true)
    public List<PatientChatMessageView> listRecentMessages(String patientId) {
        return patientChatMessageRepository.findTop20ByPatientPatientIdOrderByMessageTimeDescCreatedAtDesc(patientId)
            .stream()
            .map(message -> new PatientChatMessageView(
                message.getId(),
                message.getChatroomName(),
                message.getSenderDisplayName(),
                message.getSenderUsername(),
                message.getDirection(),
                message.getMessageType(),
                message.getContentPreview(),
                message.getContent(),
                message.getMessageTime()
            ))
            .toList();
    }

    private String buildMessageKey(WorkerPatientChatMessageReportRequest request) {
        String localId = request.localMessageId() == null ? "null" : String.valueOf(request.localMessageId());
        String serverId = request.serverMessageId() == null ? "null" : String.valueOf(request.serverMessageId());
        return trimToEmpty(request.chatroomUsername()) + ":" + localId + ":" + serverId;
    }

    private String resolveChatroomName(WorkerPatientChatMessageReportRequest request) {
        if (StringUtils.hasText(request.chatroomName())) {
            return request.chatroomName().trim();
        }
        if (StringUtils.hasText(request.chatroomDisplayName())) {
            return request.chatroomDisplayName().trim();
        }
        return trimToEmpty(request.chatroomUsername());
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String normalized = trimToEmpty(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
