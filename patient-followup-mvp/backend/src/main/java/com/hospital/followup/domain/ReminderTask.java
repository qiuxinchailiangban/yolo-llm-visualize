package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.ReminderTaskStatus;
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
@Table(name = "reminder_task")
public class ReminderTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_task_id", nullable = false)
    private QuestionnaireTask questionnaireTask;

    @Column(nullable = false, length = 64)
    private String ruleCode;

    @Column(nullable = false, length = 32)
    private String reminderChannel;

    @Column(length = 128)
    private String targetConversation;

    @Column(length = 255)
    private String contentPreview;

    @Column(nullable = false)
    private LocalDateTime plannedAt;

    private LocalDateTime startedAt;

    private LocalDateTime sentAt;

    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReminderTaskStatus status;

    @Column(length = 255)
    private String failReason;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @Column(length = 500)
    private String commandLine;

    public Long getId() {
        return id;
    }

    public QuestionnaireTask getQuestionnaireTask() {
        return questionnaireTask;
    }

    public void setQuestionnaireTask(QuestionnaireTask questionnaireTask) {
        this.questionnaireTask = questionnaireTask;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getReminderChannel() {
        return reminderChannel;
    }

    public void setReminderChannel(String reminderChannel) {
        this.reminderChannel = reminderChannel;
    }

    public String getTargetConversation() {
        return targetConversation;
    }

    public void setTargetConversation(String targetConversation) {
        this.targetConversation = targetConversation;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public ReminderTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ReminderTaskStatus status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }
}
