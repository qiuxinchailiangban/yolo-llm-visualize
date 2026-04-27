package com.hospital.followup.repository;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.enums.PatientStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    Optional<Patient> findByPatientNo(String patientNo);

    Optional<Patient> findByWechatChatroomUsername(String wechatChatroomUsername);

    List<Patient> findByNameContainingIgnoreCaseOrPatientIdContainingIgnoreCaseOrderByCreatedAtDesc(String name, String patientId);

    List<Patient> findByStatusOrderByCreatedAtDesc(PatientStatus status);

    List<Patient> findBySurgeryDateOrderByCreatedAtDesc(LocalDate surgeryDate);

    boolean existsByPatientId(String patientId);

    boolean existsByPatientNo(String patientNo);

    List<Patient> findByNameAndPhoneAndSurgeryDate(String name, String phone, LocalDate surgeryDate);

    List<Patient> findByNameAndSurgeryDate(String name, LocalDate surgeryDate);

    List<Patient> findByPhone(String phone);

    List<Patient> findByPhoneAndName(String phone, String name);
}
