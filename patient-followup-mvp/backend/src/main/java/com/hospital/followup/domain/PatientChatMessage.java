package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_chat_message")
public class PatientChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, unique = true, length = 160)
    private String messageKey;

    @Column(nullable = false, length = 128)
    private String chatroomUsername;

    @Column(length = 255)
    private String chatroomDisplayName;

    @Column(length = 255)
    private String chatroomName;

    @Column(length = 64)
    private String senderDisplayName;

    @Column(length = 128)
    private String senderUsername;

    @Column(nullable = false, length = 32)
    private String direction;

    @Column(nullable = false, length = 32)
    private String messageType;

    @Column(length = 255)
    private String contentPreview;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Long localMessageId;

    private Long serverMessageId;

    private LocalDateTime messageTime;

    @Column(length = 64)
    private String reporterWorkerId;

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getChatroomUsername() {
        return chatroomUsername;
    }

    public void setChatroomUsername(String chatroomUsername) {
        this.chatroomUsername = chatroomUsername;
    }

    public String getChatroomDisplayName() {
        return chatroomDisplayName;
    }

    public void setChatroomDisplayName(String chatroomDisplayName) {
        this.chatroomDisplayName = chatroomDisplayName;
    }

    public String getChatroomName() {
        return chatroomName;
    }

    public void setChatroomName(String chatroomName) {
        this.chatroomName = chatroomName;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getLocalMessageId() {
        return localMessageId;
    }

    public void setLocalMessageId(Long localMessageId) {
        this.localMessageId = localMessageId;
    }

    public Long getServerMessageId() {
        return serverMessageId;
    }

    public void setServerMessageId(Long serverMessageId) {
        this.serverMessageId = serverMessageId;
    }

    public LocalDateTime getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(LocalDateTime messageTime) {
        this.messageTime = messageTime;
    }

    public String getReporterWorkerId() {
        return reporterWorkerId;
    }

    public void setReporterWorkerId(String reporterWorkerId) {
        this.reporterWorkerId = reporterWorkerId;
    }
}
