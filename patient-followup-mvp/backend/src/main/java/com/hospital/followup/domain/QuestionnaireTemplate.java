package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.hospital.followup.persistence.TemplateStatusConverter;
import com.hospital.followup.persistence.TemplateTypeConverter;

@Entity
@Table(name = "questionnaire_template")
public class QuestionnaireTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String templateCode;

    @Column(nullable = false, length = 128)
    private String templateName;

    @Convert(converter = TemplateTypeConverter.class)
    @Column(nullable = false, length = 32)
    private TemplateType templateType;

    @Column(nullable = false, length = 32)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private FollowupStage stage;

    @Convert(converter = TemplateStatusConverter.class)
    @Column(nullable = false, length = 32)
    private TemplateStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String schemaJson;

    @Column(length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public FollowupStage getStage() {
        return stage;
    }

    public void setStage(FollowupStage stage) {
        this.stage = stage;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
