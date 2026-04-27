package com.hospital.followup.repository;

import com.hospital.followup.domain.PatientProcessInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProcessInstanceRepository extends JpaRepository<PatientProcessInstance, Long> {

    Optional<PatientProcessInstance> findByPatient_Id(Long patientId);

    Optional<PatientProcessInstance> findByPatient_PatientId(String patientId);

    List<PatientProcessInstance> findTop200ByOrderByUpdatedAtDesc();

    void deleteByPatient_Id(Long patientId);
}
