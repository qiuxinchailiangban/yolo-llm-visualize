package com.hospital.followup.service;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.WechatGroupLead;
import com.hospital.followup.dto.admin.CreatePatientRequest;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.dto.admin.WechatGroupLeadBindPatientRequest;
import com.hospital.followup.dto.admin.WechatGroupLeadView;
import com.hospital.followup.dto.worker.WorkerWechatGroupDiscoveryRequest;
import com.hospital.followup.dto.worker.WorkerWechatGroupDiscoveryView;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.WechatGroupLeadRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WechatGroupLeadService {

    private static final Pattern PRE_OP_PATTERN = Pattern.compile(
        "^D(?<date>\\d{1,2}\\.\\d{1,2})-(?<doctor>[^-]+)-(?<patient>[^-]+)-(?<site>.+)$"
    );
    private static final Pattern UNFIT_PATTERN = Pattern.compile(
        "^W(?<date>\\d{1,2}\\.\\d{1,2})-(?<doctor>[^-]+)-(?<patient>[^-]+)-(?<site>.+)$"
    );
    private static final Pattern POST_OP_PATTERN = Pattern.compile(
        "^(?<date>\\d{1,2}\\.\\d{1,2})-(?<doctor>[^-]+)-(?<patient>[^-]+)-(?<site>.+?)(?:\\((?<type>.+)\\))?$"
    );

    private final WechatGroupLeadRepository repository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final MessageTriggerRuleService messageTriggerRuleService;

    public WechatGroupLeadService(
        WechatGroupLeadRepository repository,
        PatientRepository patientRepository,
        PatientService patientService,
        MessageTriggerRuleService messageTriggerRuleService
    ) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
        this.messageTriggerRuleService = messageTriggerRuleService;
    }

    @Transactional
    public WorkerWechatGroupDiscoveryView registerDiscovery(WorkerWechatGroupDiscoveryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ParsedGroupLead parsed = parseGroupName(resolveRawGroupName(request));
        Optional<WechatGroupLead> existingLead = repository.findByChatroomUsername(request.chatroomUsername());
        if (!"PARSED".equals(parsed.parseStatus()) && existingLead.isEmpty()) {
            return toWorkerView(buildIgnoredLead(request, parsed, now));
        }

        WechatGroupLead lead = existingLead.orElseGet(WechatGroupLead::new);

        boolean isNew = lead.getId() == null;
        if (isNew) {
            lead.setChatroomUsername(request.chatroomUsername().trim());
            lead.setDiscoveredAt(now);
        }

        lead.setChatroomDisplayName(trimToNull(request.chatroomDisplayName()));
        lead.setRawGroupName(resolveRawGroupName(request));
        lead.setReporterWorkerId(trimToNull(request.workerId()));
        lead.setSourceChannel("WECHAT_GROUP_DISCOVERY");
        lead.setLastSeenAt(now);
        if (isNew) {
            lead.setFirstMessageSnippet(trimSnippet(request.firstMessageSnippet()));
        } else if (!StringUtils.hasText(lead.getFirstMessageSnippet())) {
            lead.setFirstMessageSnippet(trimSnippet(request.firstMessageSnippet()));
        }
        lead.setLastMessageSnippet(trimSnippet(
            StringUtils.hasText(request.lastMessageSnippet()) ? request.lastMessageSnippet() : request.firstMessageSnippet()
        ));

        applyParseResult(lead, parsed);
        WechatGroupLead saved = repository.save(lead);
        return toWorkerView(saved);
    }

    @Transactional(readOnly = true)
    public List<WechatGroupLeadView> listLeads() {
        return repository.findByParseStatusOrderByUpdatedAtDesc("PARSED").stream().map(this::toAdminView).toList();
    }

    @Transactional
    public WechatGroupLeadView createPatientFromLead(String chatroomUsername) {
        WechatGroupLead lead = loadLead(chatroomUsername);
        if (StringUtils.hasText(lead.getLinkedPatientId())) {
            throw new IllegalArgumentException("该微信群线索已绑定患者，无需重复创建");
        }
        if (!StringUtils.hasText(lead.getPatientName())) {
            throw new IllegalArgumentException("群名未解析出患者姓名，暂不能直接创建患者");
        }

        PatientView patient = patientService.createPatient(new CreatePatientRequest(
            lead.getPatientName(),
            null,
            null,
            null,
            resolveSurgeryDateForPatient(lead),
            null,
            null,
            buildDiagnosis(lead),
            "WECHAT_GROUP_LEAD"
        ));
        Patient linkedPatient = patientRepository.findByPatientId(patient.patientId())
            .or(() -> patientRepository.findByPatientNo(patient.patientId()))
            .orElseThrow(() -> new IllegalArgumentException("新创建的患者不存在"));
        attachLeadToPatient(linkedPatient, lead);
        lead.setLinkedPatientId(linkedPatient.getPatientId());
        repository.save(lead);
        messageTriggerRuleService.queueBindGroupRulesForPatient(linkedPatient);
        return toAdminView(lead);
    }

    @Transactional
    public WechatGroupLeadView bindExistingPatient(String chatroomUsername, WechatGroupLeadBindPatientRequest request) {
        WechatGroupLead lead = loadLead(chatroomUsername);
        Patient previousPatient = loadLinkedPatient(lead.getLinkedPatientId());
        Patient patient = patientRepository.findByPatientId(request.patientId())
            .or(() -> patientRepository.findByPatientNo(request.patientId()))
            .orElseThrow(() -> new IllegalArgumentException("患者不存在"));
        if (previousPatient != null && !previousPatient.getPatientId().equals(patient.getPatientId())) {
            detachLeadFromPatient(previousPatient, lead);
        }
        attachLeadToPatient(patient, lead);
        lead.setLinkedPatientId(patient.getPatientId());
        repository.save(lead);
        messageTriggerRuleService.queueBindGroupRulesForPatient(patient);
        return toAdminView(lead);
    }

    @Transactional
    public WechatGroupLeadView unbindPatient(String chatroomUsername) {
        WechatGroupLead lead = loadLead(chatroomUsername);
        Patient previousPatient = loadLinkedPatient(lead.getLinkedPatientId());
        if (previousPatient != null) {
            detachLeadFromPatient(previousPatient, lead);
        }
        lead.setLinkedPatientId(null);
        repository.save(lead);
        return toAdminView(lead);
    }

    private String resolveRawGroupName(WorkerWechatGroupDiscoveryRequest request) {
        if (StringUtils.hasText(request.rawGroupName())) {
            return request.rawGroupName().trim();
        }
        if (StringUtils.hasText(request.chatroomDisplayName())) {
            return request.chatroomDisplayName().trim();
        }
        return request.chatroomUsername().trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimSnippet(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private ParsedGroupLead parseGroupName(String rawGroupName) {
        String source = trimToNull(rawGroupName);
        if (source == null) {
            return ParsedGroupLead.failed("群名称为空，无法解析");
        }

        Optional<ParsedGroupLead> preOp = tryParse(source, PRE_OP_PATTERN, "PRE_OP");
        if (preOp.isPresent()) {
            return preOp.get();
        }
        Optional<ParsedGroupLead> unfit = tryParse(source, UNFIT_PATTERN, "UNFIT");
        if (unfit.isPresent()) {
            return unfit.get();
        }
        Optional<ParsedGroupLead> postOp = tryParse(source, POST_OP_PATTERN, "POST_OP");
        if (postOp.isPresent()) {
            return postOp.get();
        }
        return ParsedGroupLead.failed("群名未命中预设规则");
    }

    private Optional<ParsedGroupLead> tryParse(String rawGroupName, Pattern pattern, String stage) {
        Matcher matcher = pattern.matcher(rawGroupName);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String dateText = matcher.group("date");
        LocalDate eventDate = resolveDate(dateText);
        String site = trimToNull(matcher.group("site"));
        String surgeryType = null;
        if ("POST_OP".equals(stage) && site != null) {
            Matcher siteMatcher = Pattern.compile("^(?<site>.+?)(?:\\((?<type>.+)\\))?$").matcher(site);
            if (siteMatcher.matches()) {
                site = trimToNull(siteMatcher.group("site"));
                surgeryType = trimToNull(siteMatcher.group("type"));
            }
        }

        return Optional.of(new ParsedGroupLead(
            "PARSED",
            "群名解析成功",
            stage,
            dateText,
            eventDate,
            trimToNull(matcher.group("doctor")),
            trimToNull(matcher.group("patient")),
            site,
            surgeryType
        ));
    }

    private LocalDate resolveDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }
        String[] parts = rawDate.trim().split("\\.");
        if (parts.length != 2) {
            return null;
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = LocalDate.now().getYear();
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | DateTimeException ex) {
            return null;
        }
    }

    private void applyParseResult(WechatGroupLead lead, ParsedGroupLead parsed) {
        lead.setParseStatus(parsed.parseStatus());
        lead.setParseMessage(parsed.parseMessage());
        lead.setGroupStage(parsed.groupStage());
        lead.setEventDateText(parsed.eventDateText());
        lead.setEventDate(parsed.eventDate());
        lead.setAssistantDoctorName(parsed.assistantDoctorName());
        lead.setPatientName(parsed.patientName());
        lead.setSurgerySite(parsed.surgerySite());
        lead.setSurgeryType(parsed.surgeryType());
    }

    private WechatGroupLead buildIgnoredLead(
        WorkerWechatGroupDiscoveryRequest request,
        ParsedGroupLead parsed,
        LocalDateTime now
    ) {
        WechatGroupLead lead = new WechatGroupLead();
        lead.setChatroomUsername(request.chatroomUsername().trim());
        lead.setChatroomDisplayName(trimToNull(request.chatroomDisplayName()));
        lead.setRawGroupName(resolveRawGroupName(request));
        lead.setReporterWorkerId(trimToNull(request.workerId()));
        lead.setSourceChannel("WECHAT_GROUP_DISCOVERY");
        lead.setDiscoveredAt(now);
        lead.setLastSeenAt(now);
        lead.setFirstMessageSnippet(trimSnippet(request.firstMessageSnippet()));
        lead.setLastMessageSnippet(trimSnippet(
            StringUtils.hasText(request.lastMessageSnippet()) ? request.lastMessageSnippet() : request.firstMessageSnippet()
        ));
        applyParseResult(lead, parsed);
        return lead;
    }

    private WechatGroupLead loadLead(String chatroomUsername) {
        return repository.findByChatroomUsername(chatroomUsername)
            .orElseThrow(() -> new IllegalArgumentException("微信群线索不存在"));
    }

    private LocalDate resolveSurgeryDateForPatient(WechatGroupLead lead) {
        return "POST_OP".equals(lead.getGroupStage()) ? lead.getEventDate() : null;
    }

    private void attachLeadToPatient(Patient patient, WechatGroupLead lead) {
        patient.setWechatChatroomUsername(trimToNull(lead.getChatroomUsername()));
        patient.setWechatChatroomDisplayName(trimToNull(lead.getChatroomDisplayName()));
        patient.setWechatGroupName(trimToNull(lead.getRawGroupName()));
        patientRepository.save(patient);
    }

    private void detachLeadFromPatient(Patient patient, WechatGroupLead lead) {
        String leadUsername = trimToNull(lead.getChatroomUsername());
        String patientUsername = trimToNull(patient.getWechatChatroomUsername());
        if (leadUsername != null && leadUsername.equals(patientUsername)) {
            patient.setWechatChatroomUsername(null);
            patient.setWechatChatroomDisplayName(null);
            patient.setWechatGroupName(null);
            patientRepository.save(patient);
        }
    }

    private Patient loadLinkedPatient(String patientId) {
        if (!StringUtils.hasText(patientId)) {
            return null;
        }
        return patientRepository.findByPatientId(patientId)
            .or(() -> patientRepository.findByPatientNo(patientId))
            .orElse(null);
    }

    private String buildDiagnosis(WechatGroupLead lead) {
        String surgerySite = trimToNull(lead.getSurgerySite());
        String surgeryType = trimToNull(lead.getSurgeryType());
        if (surgerySite == null && surgeryType == null) {
            return null;
        }
        if (surgerySite != null && surgeryType != null) {
            return surgerySite + "(" + surgeryType + ")";
        }
        return surgerySite != null ? surgerySite : surgeryType;
    }

    private WorkerWechatGroupDiscoveryView toWorkerView(WechatGroupLead lead) {
        return new WorkerWechatGroupDiscoveryView(
            lead.getChatroomUsername(),
            lead.getChatroomDisplayName(),
            lead.getRawGroupName(),
            lead.getParseStatus(),
            lead.getParseMessage(),
            lead.getGroupStage(),
            lead.getEventDateText(),
            lead.getEventDate(),
            lead.getAssistantDoctorName(),
            lead.getPatientName(),
            lead.getSurgerySite(),
            lead.getSurgeryType(),
            lead.getLinkedPatientId(),
            lead.getDiscoveredAt(),
            lead.getLastSeenAt()
        );
    }

    private WechatGroupLeadView toAdminView(WechatGroupLead lead) {
        Patient linkedPatient = null;
        if (StringUtils.hasText(lead.getLinkedPatientId())) {
            linkedPatient = patientRepository.findByPatientId(lead.getLinkedPatientId())
                .or(() -> patientRepository.findByPatientNo(lead.getLinkedPatientId()))
                .orElse(null);
        }
        return new WechatGroupLeadView(
            lead.getChatroomUsername(),
            lead.getChatroomDisplayName(),
            lead.getRawGroupName(),
            lead.getParseStatus(),
            lead.getParseMessage(),
            lead.getGroupStage(),
            lead.getEventDateText(),
            lead.getEventDate(),
            lead.getAssistantDoctorName(),
            lead.getPatientName(),
            lead.getSurgerySite(),
            lead.getSurgeryType(),
            lead.getLinkedPatientId(),
            linkedPatient == null ? null : linkedPatient.getName(),
            lead.getReporterWorkerId(),
            lead.getSourceChannel(),
            lead.getFirstMessageSnippet(),
            lead.getLastMessageSnippet(),
            lead.getDiscoveredAt(),
            lead.getLastSeenAt(),
            lead.getUpdatedAt()
        );
    }

    private record ParsedGroupLead(
        String parseStatus,
        String parseMessage,
        String groupStage,
        String eventDateText,
        LocalDate eventDate,
        String assistantDoctorName,
        String patientName,
        String surgerySite,
        String surgeryType
    ) {
        private static ParsedGroupLead failed(String message) {
            return new ParsedGroupLead("FAILED", message, null, null, null, null, null, null, null);
        }
    }
}
