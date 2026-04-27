package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.QrCodeStatus;
import com.hospital.followup.domain.enums.QrCodeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_qrcode")
public class QuestionnaireQrCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QrCodeType qrType;

    @Column(nullable = false, unique = true, length = 32)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private QuestionnaireTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private QuestionnaireTask task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QrCodeStatus status;

    @Column(nullable = false, length = 128)
    private String pagePath;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    private Integer scanCount;

    public Long getId() {
        return id;
    }

    public QrCodeType getQrType() {
        return qrType;
    }

    public void setQrType(QrCodeType qrType) {
        this.qrType = qrType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public QuestionnaireTemplate getTemplate() {
        return template;
    }

    public void setTemplate(QuestionnaireTemplate template) {
        this.template = template;
    }

    public QuestionnaireTask getTask() {
        return task;
    }

    public void setTask(QuestionnaireTask task) {
        this.task = task;
    }

    public QrCodeStatus getStatus() {
        return status;
    }

    public void setStatus(QrCodeStatus status) {
        this.status = status;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = scanCount;
    }
}
