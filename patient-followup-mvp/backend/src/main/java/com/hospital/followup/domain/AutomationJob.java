package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.AutomationJobStatus;
import com.hospital.followup.domain.enums.AutomationJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_job")
public class AutomationJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String jobNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AutomationJobType jobType;

    @Column(nullable = false, length = 32)
    private String bizType;

    @Column(nullable = false)
    private Long bizId;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AutomationJobStatus status;

    @Column(nullable = false)
    private LocalDateTime plannedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(length = 64)
    private String workerId;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(length = 255)
    private String lastError;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    public Long getId() {
        return id;
    }

    public String getJobNo() {
        return jobNo;
    }

    public void setJobNo(String jobNo) {
        this.jobNo = jobNo;
    }

    public AutomationJobType getJobType() {
        return jobType;
    }

    public void setJobType(AutomationJobType jobType) {
        this.jobType = jobType;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public AutomationJobStatus getStatus() {
        return status;
    }

    public void setStatus(AutomationJobStatus status) {
        this.status = status;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }
}
