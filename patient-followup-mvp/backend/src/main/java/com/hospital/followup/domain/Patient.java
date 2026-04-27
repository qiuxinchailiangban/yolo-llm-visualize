package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.PatientStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import com.hospital.followup.persistence.PatientStatusConverter;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String patientId;

    @Column(name = "patient_no", length = 64)
    private String patientNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String gender;

    @Column(length = 32)
    private String phone;

    private LocalDate birthDate;

    private LocalDate surgeryDate;

    @Column(length = 64)
    private String surgeryScheduleTag;

    @Column(length = 64)
    private String surgeryTimeText;

    @Column(length = 255)
    private String diagnosis;

    @Column(length = 32)
    private String sourceChannel;

    @Column(length = 128)
    private String wechatChatroomUsername;

    @Column(length = 255)
    private String wechatChatroomDisplayName;

    @Column(length = 255)
    private String wechatGroupName;

    @Convert(converter = PatientStatusConverter.class)
    @Column(nullable = false, length = 32)
    private PatientStatus status;

    public Long getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientNo() {
        return patientNo;
    }

    public void setPatientNo(String patientNo) {
        this.patientNo = patientNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getSurgeryDate() {
        return surgeryDate;
    }

    public void setSurgeryDate(LocalDate surgeryDate) {
        this.surgeryDate = surgeryDate;
    }

    public String getSurgeryScheduleTag() {
        return surgeryScheduleTag;
    }

    public void setSurgeryScheduleTag(String surgeryScheduleTag) {
        this.surgeryScheduleTag = surgeryScheduleTag;
    }

    public String getSurgeryTimeText() {
        return surgeryTimeText;
    }

    public void setSurgeryTimeText(String surgeryTimeText) {
        this.surgeryTimeText = surgeryTimeText;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getWechatChatroomUsername() {
        return wechatChatroomUsername;
    }

    public void setWechatChatroomUsername(String wechatChatroomUsername) {
        this.wechatChatroomUsername = wechatChatroomUsername;
    }

    public String getWechatChatroomDisplayName() {
        return wechatChatroomDisplayName;
    }

    public void setWechatChatroomDisplayName(String wechatChatroomDisplayName) {
        this.wechatChatroomDisplayName = wechatChatroomDisplayName;
    }

    public String getWechatGroupName() {
        return wechatGroupName;
    }

    public void setWechatGroupName(String wechatGroupName) {
        this.wechatGroupName = wechatGroupName;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }
}
