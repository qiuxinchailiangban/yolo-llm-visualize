package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.dto.admin.StageView;
import com.hospital.followup.dto.admin.TemplateUpsertRequest;
import com.hospital.followup.dto.admin.TemplateView;
import com.hospital.followup.repository.FollowupStageRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final QuestionnaireTemplateRepository templateRepository;
    private final FollowupStageRepository stageRepository;
    private final ObjectMapper objectMapper;

    public TemplateService(
        QuestionnaireTemplateRepository templateRepository,
        FollowupStageRepository stageRepository,
        ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.stageRepository = stageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> listTemplates(String keyword, TemplateStatus status) {
        List<QuestionnaireTemplate> templates;
        if (keyword != null && !keyword.isBlank()) {
            templates = templateRepository.findByTemplateNameContainingIgnoreCaseOrTemplateCodeContainingIgnoreCaseOrderByUpdatedAtDesc(keyword, keyword);
        } else if (status != null) {
            templates = templateRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            templates = templateRepository.findAll().stream().sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())).toList();
        }
        return templates.stream().map(this::toView).toList();
    }

    @Transactional
    public TemplateView createTemplate(TemplateUpsertRequest request) {
        validateTemplateCodeUnique(request.templateCode(), null);
        QuestionnaireTemplate template = new QuestionnaireTemplate();
        fillTemplate(template, request);
        return toView(templateRepository.save(template));
    }

    @Transactional
    public TemplateView updateTemplate(Long id, TemplateUpsertRequest request) {
        validateTemplateCodeUnique(request.templateCode(), id);
        QuestionnaireTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("问卷模板不存在"));
        fillTemplate(template, request);
        return toView(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<StageView> listStages() {
        return stageRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
            .map(stage -> new StageView(
                stage.getId(),
                stage.getStageCode(),
                stage.getStageName(),
                stage.getDayOffset(),
                stage.getSortOrder(),
                stage.getEnabled(),
                stage.getReminderEnabled(),
                stage.getDescription()
            ))
            .toList();
    }

    private void fillTemplate(QuestionnaireTemplate template, TemplateUpsertRequest request) {
        validateSchemaJson(request.schemaJson());
        template.setTemplateCode(request.templateCode());
        template.setTemplateName(request.templateName());
        template.setTemplateType(request.templateType());
        template.setVersion(request.version());
        template.setStatus(request.status());
        template.setSchemaJson(request.schemaJson());
        template.setDescription(request.description());
        if (request.stageId() != null) {
            FollowupStage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new EntityNotFoundException("随访阶段不存在"));
            template.setStage(stage);
        } else {
            template.setStage(null);
        }
    }

    private void validateTemplateCodeUnique(String templateCode, Long currentId) {
        templateRepository.findByTemplateCode(templateCode)
            .filter(existing -> currentId == null || !existing.getId().equals(currentId))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("模板编码已存在，请更换一个新的模板编码");
            });
    }

    private void validateSchemaJson(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("问卷 schema 必须是 JSON 对象");
            }
            if (!root.hasNonNull("title")) {
                throw new IllegalArgumentException("问卷 schema 缺少 title");
            }
            if (!root.has("items") || !root.get("items").isArray()) {
                throw new IllegalArgumentException("问卷 schema 缺少 items 数组");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("问卷 schema 不是合法 JSON");
        }
    }

    private TemplateView toView(QuestionnaireTemplate template) {
        return new TemplateView(
            template.getId(),
            template.getTemplateCode(),
            template.getTemplateName(),
            template.getTemplateType().name(),
            template.getVersion(),
            template.getStage() == null ? null : template.getStage().getId(),
            template.getStage() == null ? null : template.getStage().getStageName(),
            template.getStatus().name(),
            template.getSchemaJson(),
            template.getDescription()
        );
    }
}
