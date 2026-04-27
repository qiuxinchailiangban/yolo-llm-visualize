package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.MessageTriggerExecution;
import com.hospital.followup.domain.MessageTriggerRule;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.PatientChatMessage;
import com.hospital.followup.dto.admin.MessageTriggerRuleConditionRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleConditionView;
import com.hospital.followup.dto.admin.MessageTriggerRuleContentBlockRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleContentBlockView;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualCandidateView;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualDetectRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteItemRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteItemView;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleManualExecuteResult;
import com.hospital.followup.dto.admin.MessageTriggerRuleMediaUploadView;
import com.hospital.followup.dto.admin.MessageTriggerRuleUpsertRequest;
import com.hospital.followup.dto.admin.MessageTriggerRuleView;
import com.hospital.followup.repository.MessageTriggerExecutionRepository;
import com.hospital.followup.repository.MessageTriggerRuleRepository;
import com.hospital.followup.repository.PatientChatMessageRepository;
import com.hospital.followup.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MessageTriggerRuleService {

    public static final String MODE_AUTO = "AUTO";
    public static final String MODE_MANUAL = "MANUAL";

    public static final String TASK_CATEGORY_PROCESS = "PROCESS";
    public static final String TASK_CATEGORY_REPLY = "REPLY";
    public static final String TASK_CATEGORY_OTHER = "OTHER";

    public static final String TRIGGER_BIND_GROUP_IMMEDIATE = "BIND_GROUP_IMMEDIATE";
    public static final String TRIGGER_SURGERY_RELATIVE_DAY = "SURGERY_RELATIVE_DAY";
    public static final String TRIGGER_KEYWORD_MESSAGE = "KEYWORD_MESSAGE";

    public static final String TARGET_BOUND_CHATROOM = "BOUND_CHATROOM";
    public static final String TARGET_PATIENT_NAME = "PATIENT_NAME";
    public static final String TARGET_CUSTOM_CONVERSATION = "CUSTOM_CONVERSATION";

    public static final String BLOCK_TEXT = "TEXT";
    public static final String BLOCK_IMAGE = "IMAGE";

    public static final String CONDITION_ALL = "ALL";
    public static final String CONDITION_ANY = "ANY";

    public static final String CONDITION_HAS_BOUND_CHATROOM = "HAS_BOUND_CHATROOM";
    public static final String CONDITION_HAS_SURGERY_DATE = "HAS_SURGERY_DATE";
    public static final String CONDITION_PATIENT_STATUS_IS = "PATIENT_STATUS_IS";
    public static final String CONDITION_PATIENT_NAME_CONTAINS = "PATIENT_NAME_CONTAINS";
    public static final String CONDITION_DIAGNOSIS_CONTAINS = "DIAGNOSIS_CONTAINS";
    public static final String CONDITION_MESSAGE_DIRECTION_IS = "MESSAGE_DIRECTION_IS";
    public static final String CONDITION_MESSAGE_CONTENT_CONTAINS = "MESSAGE_CONTENT_CONTAINS";

    public static final String KEYWORD_MATCH_ANY = "ANY";
    public static final String KEYWORD_MATCH_ALL = "ALL";

    public static final String FEEDBACK_RULE_NONE = "NONE";
    public static final String FEEDBACK_RULE_ANY_MESSAGE = "ANY_MESSAGE";
    public static final String FEEDBACK_RULE_KEYWORD = "KEYWORD";
    public static final String FEEDBACK_RULE_MANUAL_CONFIRM = "MANUAL_CONFIRM";

    private static final String EXECUTION_QUEUED = "QUEUED";
    private static final String EXECUTION_SKIPPED = "SKIPPED";
    private static final String MANUAL_ITEM_QUEUED = "QUEUED";
    private static final String MANUAL_ITEM_SKIPPED = "SKIPPED";
    private static final int DEFAULT_FEEDBACK_TIMEOUT_HOURS = 24;

    private static final Logger log = LoggerFactory.getLogger(MessageTriggerRuleService.class);
    private static final TypeReference<List<MessageTriggerRuleContentBlockView>> BLOCK_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<MessageTriggerRuleConditionView>> CONDITION_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final DateTimeFormatter RULE_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MessageTriggerRuleRepository ruleRepository;
    private final MessageTriggerExecutionRepository executionRepository;
    private final PatientRepository patientRepository;
    private final PatientChatMessageRepository patientChatMessageRepository;
    private final AutomationJobService automationJobService;
    private final ObjectMapper objectMapper;

    public MessageTriggerRuleService(
        MessageTriggerRuleRepository ruleRepository,
        MessageTriggerExecutionRepository executionRepository,
        PatientRepository patientRepository,
        PatientChatMessageRepository patientChatMessageRepository,
        AutomationJobService automationJobService,
        ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.executionRepository = executionRepository;
        this.patientRepository = patientRepository;
        this.patientChatMessageRepository = patientChatMessageRepository;
        this.automationJobService = automationJobService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<MessageTriggerRuleView> listRules() {
        return ruleRepository.findAllByOrderBySortOrderAscCreatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageTriggerRuleView> listManualRules() {
        return ruleRepository.findAllByOrderBySortOrderAscCreatedAtDesc()
            .stream()
            .filter(rule -> MODE_MANUAL.equals(rule.getRuleMode()))
            .filter(rule -> !TASK_CATEGORY_REPLY.equals(resolveTaskCategory(rule)))
            .map(this::toView)
            .toList();
    }

    @Transactional
    public MessageTriggerRuleView createRule(MessageTriggerRuleUpsertRequest request) {
        MessageTriggerRule rule = new MessageTriggerRule();
        applyRuleChanges(rule, request, true);
        return toView(ruleRepository.save(rule));
    }

    @Transactional
    public MessageTriggerRuleView updateRule(Long id, MessageTriggerRuleUpsertRequest request) {
        MessageTriggerRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("任务定义不存在"));
        applyRuleChanges(rule, request, false);
        return toView(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        MessageTriggerRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("任务定义不存在"));
        ruleRepository.delete(rule);
    }

    public MessageTriggerRuleMediaUploadView uploadMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String originalName = file.getOriginalFilename();
        String extension = resolveExtension(originalName);
        if (!List.of(".png", ".jpg", ".jpeg", ".webp").contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("目前仅支持 PNG/JPG/WEBP 图片");
        }
        Path baseDir = Path.of("runtime", "message-rule-media").toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        try {
            Files.createDirectories(targetDir);
            String sanitizedName = sanitizeFileName(originalName);
            Path targetFile = targetDir.resolve(UUID.randomUUID() + "-" + sanitizedName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return new MessageTriggerRuleMediaUploadView(targetFile.toString(), sanitizedName);
        } catch (IOException ex) {
            throw new IllegalStateException("图片上传失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public void queueBindGroupRulesForPatient(Patient patient) {
        if (patient == null) {
            return;
        }
        for (MessageTriggerRule rule : activeAutoRules()) {
            if (!TRIGGER_BIND_GROUP_IMMEDIATE.equals(rule.getTriggerType())) {
                continue;
            }
            Optional<Candidate> candidate = detectBindCandidate(rule, patient);
            candidate.ifPresent(this::queueCandidate);
        }
    }

    @Transactional
    public void scanAndQueueDueRules() {
        List<Patient> patients = patientRepository.findAll();
        for (MessageTriggerRule rule : activeAutoRules()) {
            if (!TRIGGER_SURGERY_RELATIVE_DAY.equals(rule.getTriggerType())) {
                continue;
            }
            for (Patient patient : patients) {
                detectSurgeryCandidate(rule, patient).ifPresent(this::queueCandidate);
            }
        }
    }

    @Transactional
    public void queueKeywordRulesForMessage(Patient patient, PatientChatMessage message) {
        if (patient == null || message == null) {
            return;
        }
        for (MessageTriggerRule rule : activeAutoRules()) {
            if (!TRIGGER_KEYWORD_MESSAGE.equals(rule.getTriggerType())) {
                continue;
            }
            detectKeywordCandidate(rule, patient, message).ifPresent(this::queueCandidate);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageTriggerRuleManualCandidateView> detectManualCandidates(MessageTriggerRuleManualDetectRequest request) {
        List<MessageTriggerRule> rules = resolveManualRules(request.ruleIds());
        List<Patient> patients = resolvePatients(request.patientIds());
        List<MessageTriggerRuleManualCandidateView> result = new ArrayList<>();
        for (MessageTriggerRule rule : rules) {
            for (Patient patient : patients) {
                detectCandidates(rule, patient, null).stream()
                    .map(this::toManualCandidateView)
                    .forEach(result::add);
            }
        }
        return result;
    }

    @Transactional
    public MessageTriggerRuleManualExecuteResult executeManualCandidates(MessageTriggerRuleManualExecuteRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一条手动任务");
        }
        int queued = 0;
        int skipped = 0;
        List<MessageTriggerRuleManualExecuteItemView> items = new ArrayList<>();
        for (MessageTriggerRuleManualExecuteItemRequest item : request.items()) {
            MessageTriggerRule rule = loadRule(item.ruleId());
            if (!MODE_MANUAL.equals(rule.getRuleMode())) {
                skipped++;
                items.add(new MessageTriggerRuleManualExecuteItemView(
                    item.candidateKey(),
                    rule.getRuleName(),
                    item.patientId(),
                    item.patientId(),
                    MANUAL_ITEM_SKIPPED,
                    "该规则不是手动模式"
                ));
                continue;
            }
            Patient patient = loadPatient(item.patientId());
            Optional<Candidate> candidate = detectCandidates(rule, patient, trimToNull(item.sourceMessageKey())).stream()
                .filter(found -> found.triggerKey().equals(item.candidateKey()))
                .findFirst();
            if (candidate.isEmpty()) {
                skipped++;
                items.add(new MessageTriggerRuleManualExecuteItemView(
                    item.candidateKey(),
                    rule.getRuleName(),
                    resolvePatientIdentity(patient),
                    safe(patient.getName()),
                    MANUAL_ITEM_SKIPPED,
                    "当前未检测到可执行候选，可能已发送或条件已变化"
                ));
                continue;
            }
            queueCandidate(candidate.get());
            queued++;
            items.add(new MessageTriggerRuleManualExecuteItemView(
                candidate.get().triggerKey(),
                rule.getRuleName(),
                resolvePatientIdentity(patient),
                safe(patient.getName()),
                MANUAL_ITEM_QUEUED,
                "已加入自动化任务队列"
            ));
        }
        return new MessageTriggerRuleManualExecuteResult(request.items().size(), queued, skipped, items);
    }

    private List<MessageTriggerRule> activeAutoRules() {
        return ruleRepository.findByEnabledTrueOrderBySortOrderAscCreatedAtAsc()
            .stream()
            .filter(rule -> MODE_AUTO.equals(rule.getRuleMode()))
            .toList();
    }

    private List<MessageTriggerRule> resolveManualRules(Collection<Long> ruleIds) {
        return ruleRepository.findAllByOrderBySortOrderAscCreatedAtDesc().stream()
            .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
            .filter(rule -> MODE_MANUAL.equals(rule.getRuleMode()))
            .filter(rule -> ruleIds == null || ruleIds.isEmpty() || ruleIds.contains(rule.getId()))
            .toList();
    }

    private List<Patient> resolvePatients(Collection<String> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return patientRepository.findAll();
        }
        return patientIds.stream()
            .map(this::loadPatient)
            .toList();
    }

    private MessageTriggerRule loadRule(Long ruleId) {
        return ruleRepository.findById(ruleId)
            .orElseThrow(() -> new EntityNotFoundException("任务定义不存在"));
    }

    private Patient loadPatient(String patientId) {
        return patientRepository.findByPatientId(patientId)
            .or(() -> patientRepository.findByPatientNo(patientId))
            .orElseThrow(() -> new EntityNotFoundException("患者不存在"));
    }

    private void applyRuleChanges(MessageTriggerRule rule, MessageTriggerRuleUpsertRequest request, boolean creating) {
        validateRequest(request);
        rule.setRuleCode(resolveRuleCode(rule, request, creating));
        rule.setRuleName(request.ruleName().trim());
        rule.setRuleMode(normalizeRuleMode(request.ruleMode()));
        rule.setTaskCategory(normalizeTaskCategory(request.taskCategory()));
        rule.setTriggerType(normalizeTriggerType(request.triggerType()));
        rule.setTriggerConfigJson(writeJson(buildTriggerConfig(request)));
        rule.setConditionRelation(normalizeConditionRelation(request.conditionRelation()));
        rule.setConditionConfigJson(writeJson(normalizeConditions(request.conditions())));
        rule.setTargetType(normalizeTargetType(request.targetType()));
        rule.setCustomTargetConversation(trimToNull(request.customTargetConversation()));
        rule.setContentConfigJson(writeJson(normalizeBlocks(request.contentBlocks())));
        rule.setFeedbackRequired(request.feedbackRequired() == null ? Boolean.FALSE : request.feedbackRequired());
        rule.setFeedbackRule(normalizeFeedbackRule(request.feedbackRule(), request.feedbackRequired()));
        rule.setFeedbackKeywordText(normalizeFeedbackKeywordText(request.feedbackKeywordText(), rule.getFeedbackRule()));
        rule.setFeedbackTimeoutHours(resolveFeedbackTimeoutHours(request.feedbackTimeoutHours(), rule.getFeedbackRequired()));
        rule.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
        rule.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        rule.setDescription(trimToNull(request.description()));
    }

    private void validateRequest(MessageTriggerRuleUpsertRequest request) {
        String triggerType = normalizeTriggerType(request.triggerType());
        normalizeRuleMode(request.ruleMode());
        String taskCategory = normalizeTaskCategory(request.taskCategory());
        normalizeConditionRelation(request.conditionRelation());
        if (TRIGGER_SURGERY_RELATIVE_DAY.equals(triggerType) && request.relativeDayOffset() == null) {
            throw new IllegalArgumentException("相对手术日触发必须填写天数");
        }
        if (TRIGGER_KEYWORD_MESSAGE.equals(triggerType) && parseKeywordList(request.keywordText()).isEmpty()) {
            throw new IllegalArgumentException("关键词触发必须填写至少一个关键词");
        }
        if (TASK_CATEGORY_REPLY.equals(taskCategory) && !TRIGGER_KEYWORD_MESSAGE.equals(triggerType)) {
            throw new IllegalArgumentException("回复任务目前仅支持关键词消息触发");
        }
        String targetType = normalizeTargetType(request.targetType());
        if (TARGET_CUSTOM_CONVERSATION.equals(targetType) && !StringUtils.hasText(request.customTargetConversation())) {
            throw new IllegalArgumentException("自定义会话不能为空");
        }
        List<MessageTriggerRuleContentBlockView> blocks = normalizeBlocks(request.contentBlocks());
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("请至少配置一个内容模块");
        }
        boolean hasAnyContent = blocks.stream().anyMatch(block ->
            (BLOCK_TEXT.equals(block.blockType()) && StringUtils.hasText(block.textContent()))
                || (BLOCK_IMAGE.equals(block.blockType()) && StringUtils.hasText(block.mediaPath()))
        );
        if (!hasAnyContent) {
            throw new IllegalArgumentException("请至少填写一段文字或上传图片");
        }
        boolean feedbackRequired = Boolean.TRUE.equals(request.feedbackRequired());
        String feedbackRule = normalizeFeedbackRule(request.feedbackRule(), request.feedbackRequired());
        if (feedbackRequired && FEEDBACK_RULE_KEYWORD.equals(feedbackRule) && parseKeywordList(request.feedbackKeywordText()).isEmpty()) {
            throw new IllegalArgumentException("关键词反馈规则至少要填写一个反馈关键词");
        }
        if (request.feedbackTimeoutHours() != null && request.feedbackTimeoutHours() < 1) {
            throw new IllegalArgumentException("反馈超时小时数必须大于 0");
        }
        normalizeConditions(request.conditions());
    }

    private String resolveRuleCode(MessageTriggerRule rule, MessageTriggerRuleUpsertRequest request, boolean creating) {
        String requested = trimToNull(request.ruleCode());
        if (requested != null) {
            return requested;
        }
        if (!creating && StringUtils.hasText(rule.getRuleCode())) {
            return rule.getRuleCode();
        }
        return "MSG_RULE_" + RULE_CODE_TIME.format(LocalDateTime.now());
    }

    private Map<String, Object> buildTriggerConfig(MessageTriggerRuleUpsertRequest request) {
        Map<String, Object> config = new HashMap<>();
        if (request.relativeDayOffset() != null) {
            config.put("relativeDayOffset", request.relativeDayOffset());
        }
        List<String> keywords = parseKeywordList(request.keywordText());
        if (!keywords.isEmpty()) {
            config.put("keywords", keywords);
        }
        String keywordMatchMode = trimToNull(request.keywordMatchMode());
        if (keywordMatchMode != null) {
            config.put("keywordMatchMode", normalizeKeywordMatchMode(keywordMatchMode));
        }
        return config;
    }

    private List<MessageTriggerRuleContentBlockView> normalizeBlocks(List<MessageTriggerRuleContentBlockRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<MessageTriggerRuleContentBlockView> blocks = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            MessageTriggerRuleContentBlockRequest item = requests.get(index);
            if (item == null) {
                continue;
            }
            String blockType = normalizeBlockType(item.blockType());
            blocks.add(new MessageTriggerRuleContentBlockView(
                blockType,
                BLOCK_TEXT.equals(blockType) ? trimToNull(item.textContent()) : null,
                BLOCK_IMAGE.equals(blockType) ? trimToNull(item.mediaPath()) : null,
                BLOCK_IMAGE.equals(blockType) ? trimToNull(item.mediaName()) : null,
                item.sortOrder() == null ? index + 1 : item.sortOrder()
            ));
        }
        return blocks.stream().sorted(Comparator.comparing(MessageTriggerRuleContentBlockView::sortOrder)).toList();
    }

    private List<MessageTriggerRuleConditionView> normalizeConditions(List<MessageTriggerRuleConditionRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<MessageTriggerRuleConditionView> conditions = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            MessageTriggerRuleConditionRequest item = requests.get(index);
            if (item == null) {
                continue;
            }
            String conditionType = normalizeConditionType(item.conditionType());
            conditions.add(new MessageTriggerRuleConditionView(
                conditionType,
                trimToNull(item.conditionValue()),
                item.sortOrder() == null ? index + 1 : item.sortOrder()
            ));
        }
        return conditions.stream().sorted(Comparator.comparing(MessageTriggerRuleConditionView::sortOrder)).toList();
    }

    private MessageTriggerRuleView toView(MessageTriggerRule rule) {
        return new MessageTriggerRuleView(
            rule.getId(),
            rule.getRuleCode(),
            rule.getRuleName(),
            rule.getRuleMode(),
            resolveTaskCategory(rule),
            rule.getTriggerType(),
            readRelativeDayOffset(rule),
            readKeywordText(rule),
            readKeywordMatchMode(rule),
            rule.getConditionRelation(),
            readConditions(rule),
            rule.getTargetType(),
            rule.getCustomTargetConversation(),
            readBlocks(rule),
            resolveFeedbackRequired(rule),
            resolveFeedbackRule(rule),
            readFeedbackKeywordText(rule),
            resolveFeedbackTimeoutHours(rule),
            rule.getEnabled(),
            rule.getSortOrder(),
            rule.getDescription(),
            rule.getLastTriggeredAt(),
            rule.getCreatedAt(),
            rule.getUpdatedAt()
        );
    }

    private List<Candidate> detectCandidates(MessageTriggerRule rule, Patient patient, String sourceMessageKey) {
        return switch (rule.getTriggerType()) {
            case TRIGGER_BIND_GROUP_IMMEDIATE -> detectBindCandidate(rule, patient).stream().toList();
            case TRIGGER_SURGERY_RELATIVE_DAY -> detectSurgeryCandidate(rule, patient).stream().toList();
            case TRIGGER_KEYWORD_MESSAGE -> detectKeywordCandidates(rule, patient, sourceMessageKey);
            default -> List.of();
        };
    }

    private Optional<Candidate> detectBindCandidate(MessageTriggerRule rule, Patient patient) {
        String chatroomKey = normalize(resolveBoundChatroomTarget(patient));
        if (chatroomKey.isEmpty()) {
            return Optional.empty();
        }
        String triggerKey = rule.getRuleCode() + ":" + resolvePatientIdentity(patient) + ":" + chatroomKey;
        return buildCandidate(rule, patient, null, triggerKey, "患者已绑定微信群");
    }

    private Optional<Candidate> detectSurgeryCandidate(MessageTriggerRule rule, Patient patient) {
        Integer relativeDay = readRelativeDayOffset(rule);
        if (relativeDay == null || patient.getSurgeryDate() == null) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now();
        LocalDate targetDate = patient.getSurgeryDate().plusDays(relativeDay);
        if (!targetDate.equals(today)) {
            return Optional.empty();
        }
        String triggerKey = rule.getRuleCode() + ":" + resolvePatientIdentity(patient) + ":" + targetDate;
        String reason = relativeDay == 0 ? "手术当天命中" : (relativeDay > 0 ? "术后第 " + relativeDay + " 天命中" : "术前 " + Math.abs(relativeDay) + " 天命中");
        return buildCandidate(rule, patient, null, triggerKey, reason);
    }

    private Optional<Candidate> detectKeywordCandidate(MessageTriggerRule rule, Patient patient, PatientChatMessage message) {
        if (message == null) {
            return Optional.empty();
        }
        if (!keywordMatches(rule, message)) {
            return Optional.empty();
        }
        String triggerKey = rule.getRuleCode() + ":" + resolvePatientIdentity(patient) + ":" + message.getMessageKey();
        return buildCandidate(rule, patient, message, triggerKey, "患者消息命中关键词");
    }

    private List<Candidate> detectKeywordCandidates(MessageTriggerRule rule, Patient patient, String sourceMessageKey) {
        List<PatientChatMessage> messages;
        if (StringUtils.hasText(sourceMessageKey)) {
            messages = patientChatMessageRepository.findByMessageKey(sourceMessageKey).stream().toList();
        } else {
            messages = patientChatMessageRepository.findTop20ByPatientPatientIdOrderByMessageTimeDescCreatedAtDesc(resolvePatientIdentity(patient));
        }
        List<Candidate> candidates = new ArrayList<>();
        for (PatientChatMessage message : messages) {
            detectKeywordCandidate(rule, patient, message).ifPresent(candidates::add);
        }
        return candidates;
    }

    private Optional<Candidate> buildCandidate(
        MessageTriggerRule rule,
        Patient patient,
        PatientChatMessage message,
        String triggerKey,
        String detectedReason
    ) {
        if (executionRepository.existsByTriggerKey(triggerKey)) {
            return Optional.empty();
        }
        if (!conditionsMatch(rule, patient, message)) {
            return Optional.empty();
        }
        MessagePayload payload = buildMessagePayload(rule, patient);
        return Optional.of(new Candidate(rule, patient, message, triggerKey, detectedReason, payload));
    }

    private void queueCandidate(Candidate candidate) {
        if (executionRepository.existsByTriggerKey(candidate.triggerKey())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MessagePayload payload = candidate.payload();
        MessageTriggerExecution execution = new MessageTriggerExecution();
        execution.setRule(candidate.rule());
        execution.setPatient(candidate.patient());
        execution.setTriggerKey(candidate.triggerKey());
        execution.setTriggerType(candidate.rule().getTriggerType());
        execution.setTriggeredAt(now);
        execution.setPlannedAt(now);
        execution.setTargetConversation(payload.targetConversation());
        execution.setContentPreview(payload.preview());
        execution.setImagePath(payload.imagePaths().isEmpty() ? null : payload.imagePaths().getFirst());
        execution.setExecutionLog(appendLog("", "命中规则: " + candidate.rule().getRuleName()));
        execution.setExecutionLog(appendLog(execution.getExecutionLog(), candidate.detectedReason()));

        if (!payload.sendable()) {
            execution.setStatus(EXECUTION_SKIPPED);
            execution.setErrorMessage(payload.reason());
            execution.setExecutionLog(appendLog(execution.getExecutionLog(), "跳过发送: " + payload.reason()));
            executionRepository.save(execution);
            return;
        }

        execution.setStatus(EXECUTION_QUEUED);
        execution.setExecutionLog(appendLog(execution.getExecutionLog(), "目标会话: " + payload.targetConversation()));
        execution.setExecutionLog(appendLog(execution.getExecutionLog(), "已准备加入自动化任务队列"));
        execution = executionRepository.save(execution);
        String automationJobNo = automationJobService.createMessageTriggerJob(
            execution,
            payload.targetConversation(),
            payload.content(),
            0,
            now,
            payload.imagePaths()
        ).getJobNo();
        execution.setAutomationJobNo(automationJobNo);
        execution.setExecutionLog(appendLog(execution.getExecutionLog(), "自动化任务已创建: " + automationJobNo));
        candidate.rule().setLastTriggeredAt(now);
        log.info(
            "[message-trigger-rule] queued rule={} patient={} target={}",
            candidate.rule().getRuleCode(),
            resolvePatientIdentity(candidate.patient()),
            payload.targetConversation()
        );
    }

    private boolean conditionsMatch(MessageTriggerRule rule, Patient patient, PatientChatMessage message) {
        List<MessageTriggerRuleConditionView> conditions = readConditions(rule);
        if (conditions.isEmpty()) {
            return true;
        }
        boolean relationAll = CONDITION_ALL.equals(normalizeConditionRelation(rule.getConditionRelation()));
        boolean matchedAny = false;
        for (MessageTriggerRuleConditionView condition : conditions) {
            boolean matched = matchesCondition(condition, patient, message);
            if (relationAll && !matched) {
                return false;
            }
            if (!relationAll && matched) {
                return true;
            }
            matchedAny = matchedAny || matched;
        }
        return relationAll || matchedAny;
    }

    private boolean matchesCondition(MessageTriggerRuleConditionView condition, Patient patient, PatientChatMessage message) {
        String value = safe(condition.conditionValue());
        return switch (condition.conditionType()) {
            case CONDITION_HAS_BOUND_CHATROOM -> parseExpectedBoolean(value, true)
                == StringUtils.hasText(resolveBoundChatroomTarget(patient));
            case CONDITION_HAS_SURGERY_DATE -> parseExpectedBoolean(value, true)
                == (patient.getSurgeryDate() != null);
            case CONDITION_PATIENT_STATUS_IS -> safe(patient.getStatus() == null ? null : patient.getStatus().name()).equalsIgnoreCase(value);
            case CONDITION_PATIENT_NAME_CONTAINS -> containsIgnoreCase(patient.getName(), value);
            case CONDITION_DIAGNOSIS_CONTAINS -> containsIgnoreCase(patient.getDiagnosis(), value);
            case CONDITION_MESSAGE_DIRECTION_IS -> message != null && safe(message.getDirection()).equalsIgnoreCase(value);
            case CONDITION_MESSAGE_CONTENT_CONTAINS -> message != null && containsIgnoreCase(message.getContent(), value);
            default -> true;
        };
    }

    private boolean keywordMatches(MessageTriggerRule rule, PatientChatMessage message) {
        List<String> keywords = readKeywords(rule);
        if (keywords.isEmpty()) {
            return false;
        }
        String content = safe(message.getContent());
        if (content.isEmpty()) {
            return false;
        }
        String matchMode = readKeywordMatchMode(rule);
        if (KEYWORD_MATCH_ALL.equals(matchMode)) {
            return keywords.stream().allMatch(keyword -> containsIgnoreCase(content, keyword));
        }
        return keywords.stream().anyMatch(keyword -> containsIgnoreCase(content, keyword));
    }

    private MessagePayload buildMessagePayload(MessageTriggerRule rule, Patient patient) {
        String targetConversation = resolveTargetConversation(rule, patient);
        List<MessageTriggerRuleContentBlockView> blocks = readBlocks(rule);
        StringBuilder contentBuilder = new StringBuilder();
        List<String> imagePaths = new ArrayList<>();
        for (MessageTriggerRuleContentBlockView block : blocks) {
            if (BLOCK_TEXT.equals(block.blockType()) && StringUtils.hasText(block.textContent())) {
                if (!contentBuilder.isEmpty()) {
                    contentBuilder.append(System.lineSeparator()).append(System.lineSeparator());
                }
                contentBuilder.append(renderText(block.textContent(), patient));
            }
            if (BLOCK_IMAGE.equals(block.blockType()) && StringUtils.hasText(block.mediaPath())) {
                imagePaths.add(block.mediaPath().trim());
            }
        }
        String content = contentBuilder.toString().trim();
        String preview = !content.isEmpty() ? truncate(content, 255) : (imagePaths.isEmpty() ? "" : "图片 x" + imagePaths.size());
        if (!StringUtils.hasText(targetConversation)) {
            return new MessagePayload("", content, imagePaths, preview, false, "未找到可发送的目标会话");
        }
        if (!StringUtils.hasText(content) && imagePaths.isEmpty()) {
            return new MessagePayload(targetConversation, content, imagePaths, preview, false, "规则内容为空");
        }
        return new MessagePayload(targetConversation, content, imagePaths, preview, true, null);
    }

    private String renderText(String template, Patient patient) {
        String result = template == null ? "" : template;
        return result
            .replace("{{patientName}}", safe(patient.getName()))
            .replace("{{patientId}}", safe(resolvePatientIdentity(patient)))
            .replace("{{surgeryDate}}", patient.getSurgeryDate() == null ? "" : patient.getSurgeryDate().toString())
            .replace("{{groupName}}", safe(patient.getWechatGroupName()));
    }

    private String resolveTargetConversation(MessageTriggerRule rule, Patient patient) {
        return switch (normalizeTargetType(rule.getTargetType())) {
            case TARGET_BOUND_CHATROOM -> resolveBoundChatroomTarget(patient);
            case TARGET_PATIENT_NAME -> safe(patient.getName());
            case TARGET_CUSTOM_CONVERSATION -> safe(rule.getCustomTargetConversation());
            default -> "";
        };
    }

    private String resolveBoundChatroomTarget(Patient patient) {
        String groupName = safe(patient.getWechatGroupName());
        if (!groupName.isEmpty()) {
            return groupName;
        }
        String displayName = safe(patient.getWechatChatroomDisplayName());
        if (!displayName.isEmpty()) {
            return displayName;
        }
        return "";
    }

    private Integer readRelativeDayOffset(MessageTriggerRule rule) {
        Object raw = readMap(rule.getTriggerConfigJson()).get("relativeDayOffset");
        return raw instanceof Number number ? number.intValue() : null;
    }

    private List<String> readKeywords(MessageTriggerRule rule) {
        Object raw = readMap(rule.getTriggerConfigJson()).get("keywords");
        if (raw instanceof List<?> list) {
            return list.stream().map(item -> safe(String.valueOf(item))).filter(value -> !value.isEmpty()).toList();
        }
        return List.of();
    }

    private String readKeywordText(MessageTriggerRule rule) {
        return String.join(System.lineSeparator(), readKeywords(rule));
    }

    private String readKeywordMatchMode(MessageTriggerRule rule) {
        Object raw = readMap(rule.getTriggerConfigJson()).get("keywordMatchMode");
        return raw == null ? KEYWORD_MATCH_ANY : normalizeKeywordMatchMode(String.valueOf(raw));
    }

    private List<MessageTriggerRuleContentBlockView> readBlocks(MessageTriggerRule rule) {
        if (!StringUtils.hasText(rule.getContentConfigJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rule.getContentConfigJson(), BLOCK_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消息规则内容解析失败", ex);
        }
    }

    private List<MessageTriggerRuleConditionView> readConditions(MessageTriggerRule rule) {
        if (!StringUtils.hasText(rule.getConditionConfigJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rule.getConditionConfigJson(), CONDITION_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消息规则条件解析失败", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消息规则触发配置解析失败", ex);
        }
    }

    private MessageTriggerRuleManualCandidateView toManualCandidateView(Candidate candidate) {
        PatientChatMessage sourceMessage = candidate.message();
        return new MessageTriggerRuleManualCandidateView(
            candidate.triggerKey(),
            candidate.rule().getId(),
            candidate.rule().getRuleName(),
            candidate.rule().getTriggerType(),
            resolvePatientIdentity(candidate.patient()),
            safe(candidate.patient().getName()),
            candidate.payload().targetConversation(),
            candidate.payload().preview(),
            sourceMessage == null ? null : sourceMessage.getMessageKey(),
            sourceMessage == null ? null : truncate(safe(sourceMessage.getContent()), 255),
            candidate.detectedReason()
        );
    }

    private String normalizeRuleMode(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!List.of(MODE_AUTO, MODE_MANUAL).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的规则模式: " + value);
        }
        return normalized;
    }

    private String normalizeTaskCategory(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return TASK_CATEGORY_PROCESS;
        }
        if (!List.of(TASK_CATEGORY_PROCESS, TASK_CATEGORY_REPLY, TASK_CATEGORY_OTHER).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的任务分类: " + value);
        }
        return normalized;
    }

    private String normalizeTriggerType(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!List.of(TRIGGER_BIND_GROUP_IMMEDIATE, TRIGGER_SURGERY_RELATIVE_DAY, TRIGGER_KEYWORD_MESSAGE).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的触发类型: " + value);
        }
        return normalized;
    }

    private String normalizeTargetType(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!List.of(TARGET_BOUND_CHATROOM, TARGET_PATIENT_NAME, TARGET_CUSTOM_CONVERSATION).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的发送对象: " + value);
        }
        return normalized;
    }

    private String normalizeBlockType(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!List.of(BLOCK_TEXT, BLOCK_IMAGE).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的内容模块类型: " + value);
        }
        return normalized;
    }

    private String normalizeConditionType(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!List.of(
            CONDITION_HAS_BOUND_CHATROOM,
            CONDITION_HAS_SURGERY_DATE,
            CONDITION_PATIENT_STATUS_IS,
            CONDITION_PATIENT_NAME_CONTAINS,
            CONDITION_DIAGNOSIS_CONTAINS,
            CONDITION_MESSAGE_DIRECTION_IS,
            CONDITION_MESSAGE_CONTENT_CONTAINS
        ).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的条件类型: " + value);
        }
        return normalized;
    }

    private String normalizeConditionRelation(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return CONDITION_ALL;
        }
        if (!List.of(CONDITION_ALL, CONDITION_ANY).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的条件关系: " + value);
        }
        return normalized;
    }

    private String normalizeKeywordMatchMode(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return KEYWORD_MATCH_ANY;
        }
        if (!List.of(KEYWORD_MATCH_ANY, KEYWORD_MATCH_ALL).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的关键词匹配方式: " + value);
        }
        return normalized;
    }

    private String normalizeFeedbackRule(String value, Boolean feedbackRequired) {
        if (!Boolean.TRUE.equals(feedbackRequired)) {
            return FEEDBACK_RULE_NONE;
        }
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return FEEDBACK_RULE_ANY_MESSAGE;
        }
        if (!List.of(FEEDBACK_RULE_NONE, FEEDBACK_RULE_ANY_MESSAGE, FEEDBACK_RULE_KEYWORD, FEEDBACK_RULE_MANUAL_CONFIRM).contains(normalized)) {
            throw new IllegalArgumentException("暂不支持的反馈规则: " + value);
        }
        return normalized;
    }

    private String normalizeFeedbackKeywordText(String value, String feedbackRule) {
        if (!FEEDBACK_RULE_KEYWORD.equals(feedbackRule)) {
            return null;
        }
        String joined = String.join(System.lineSeparator(), parseKeywordList(value));
        return joined.isEmpty() ? null : joined;
    }

    private Integer resolveFeedbackTimeoutHours(Integer value, Boolean feedbackRequired) {
        if (!Boolean.TRUE.equals(feedbackRequired)) {
            return null;
        }
        return value == null ? DEFAULT_FEEDBACK_TIMEOUT_HOURS : value;
    }

    public String resolveTaskCategory(MessageTriggerRule rule) {
        if (rule == null) {
            return TASK_CATEGORY_PROCESS;
        }
        if (StringUtils.hasText(rule.getTaskCategory())) {
            return normalizeTaskCategory(rule.getTaskCategory());
        }
        return TRIGGER_KEYWORD_MESSAGE.equals(rule.getTriggerType()) ? TASK_CATEGORY_REPLY : TASK_CATEGORY_PROCESS;
    }

    public boolean resolveFeedbackRequired(MessageTriggerRule rule) {
        return rule != null && Boolean.TRUE.equals(rule.getFeedbackRequired());
    }

    public String resolveFeedbackRule(MessageTriggerRule rule) {
        if (!resolveFeedbackRequired(rule)) {
            return FEEDBACK_RULE_NONE;
        }
        return normalizeFeedbackRule(rule == null ? null : rule.getFeedbackRule(), Boolean.TRUE);
    }

    public String readFeedbackKeywordText(MessageTriggerRule rule) {
        return FEEDBACK_RULE_KEYWORD.equals(resolveFeedbackRule(rule)) ? trimToNull(rule == null ? null : rule.getFeedbackKeywordText()) : null;
    }

    public List<String> readFeedbackKeywords(MessageTriggerRule rule) {
        return parseKeywordList(readFeedbackKeywordText(rule));
    }

    public Integer resolveFeedbackTimeoutHours(MessageTriggerRule rule) {
        return resolveFeedbackRequired(rule)
            ? (rule != null && rule.getFeedbackTimeoutHours() != null ? rule.getFeedbackTimeoutHours() : DEFAULT_FEEDBACK_TIMEOUT_HOURS)
            : null;
    }

    private List<String> parseKeywordList(String keywordText) {
        if (!StringUtils.hasText(keywordText)) {
            return List.of();
        }
        return Arrays.stream(keywordText.split("[,，\\n\\r]+"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .distinct()
            .toList();
    }

    private String appendLog(String original, String message) {
        String line = "[" + LocalDateTime.now() + "] " + message;
        if (!StringUtils.hasText(original)) {
            return line;
        }
        return original + System.lineSeparator() + line;
    }

    private boolean parseExpectedBoolean(String value, boolean defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private boolean containsIgnoreCase(String source, String expected) {
        return safe(source).toLowerCase(Locale.ROOT).contains(safe(expected).toLowerCase(Locale.ROOT));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolvePatientIdentity(Patient patient) {
        if (StringUtils.hasText(patient.getPatientId())) {
            return patient.getPatientId().trim();
        }
        return safe(patient.getPatientNo());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消息规则配置序列化失败", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String sanitizeFileName(String originalName) {
        String raw = originalName == null ? "image" : originalName.trim();
        String sanitized = raw.replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.isBlank() ? "image" : sanitized;
    }

    private String resolveExtension(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }

    private record MessagePayload(
        String targetConversation,
        String content,
        List<String> imagePaths,
        String preview,
        boolean sendable,
        String reason
    ) {
    }

    private record Candidate(
        MessageTriggerRule rule,
        Patient patient,
        PatientChatMessage message,
        String triggerKey,
        String detectedReason,
        MessagePayload payload
    ) {
    }
}
