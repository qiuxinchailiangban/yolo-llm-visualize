package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wechat_group_lead")
public class WechatGroupLead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String chatroomUsername;

    @Column(length = 255)
    private String chatroomDisplayName;

    @Column(length = 255)
    private String rawGroupName;

    @Column(nullable = false, length = 32)
    private String parseStatus;

    @Column(length = 255)
    private String parseMessage;

    @Column(length = 32)
    private String groupStage;

    @Column(length = 16)
    private String eventDateText;

    private LocalDate eventDate;

    @Column(length = 64)
    private String assistantDoctorName;

    @Column(length = 64)
    private String patientName;

    @Column(length = 128)
    private String surgerySite;

    @Column(length = 64)
    private String surgeryType;

    @Column(length = 64)
    private String sourceChannel;

    @Column(length = 64)
    private String reporterWorkerId;

    @Column(length = 255)
    private String firstMessageSnippet;

    @Column(length = 255)
    private String lastMessageSnippet;

    private LocalDateTime discoveredAt;

    private LocalDateTime lastSeenAt;

    @Column(length = 32)
    private String linkedPatientId;

    public Long getId() {
        return id;
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

    public String getRawGroupName() {
        return rawGroupName;
    }

    public void setRawGroupName(String rawGroupName) {
        this.rawGroupName = rawGroupName;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseMessage() {
        return parseMessage;
    }

    public void setParseMessage(String parseMessage) {
        this.parseMessage = parseMessage;
    }

    public String getGroupStage() {
        return groupStage;
    }

    public void setGroupStage(String groupStage) {
        this.groupStage = groupStage;
    }

    public String getEventDateText() {
        return eventDateText;
    }

    public void setEventDateText(String eventDateText) {
        this.eventDateText = eventDateText;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getAssistantDoctorName() {
        return assistantDoctorName;
    }

    public void setAssistantDoctorName(String assistantDoctorName) {
        this.assistantDoctorName = assistantDoctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getSurgerySite() {
        return surgerySite;
    }

    public void setSurgerySite(String surgerySite) {
        this.surgerySite = surgerySite;
    }

    public String getSurgeryType() {
        return surgeryType;
    }

    public void setSurgeryType(String surgeryType) {
        this.surgeryType = surgeryType;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getReporterWorkerId() {
        return reporterWorkerId;
    }

    public void setReporterWorkerId(String reporterWorkerId) {
        this.reporterWorkerId = reporterWorkerId;
    }

    public String getFirstMessageSnippet() {
        return firstMessageSnippet;
    }

    public void setFirstMessageSnippet(String firstMessageSnippet) {
        this.firstMessageSnippet = firstMessageSnippet;
    }

    public String getLastMessageSnippet() {
        return lastMessageSnippet;
    }

    public void setLastMessageSnippet(String lastMessageSnippet) {
        this.lastMessageSnippet = lastMessageSnippet;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getLinkedPatientId() {
        return linkedPatientId;
    }

    public void setLinkedPatientId(String linkedPatientId) {
        this.linkedPatientId = linkedPatientId;
    }
}
