package com.hospital.followup.repository;

import com.hospital.followup.domain.PatientChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientChatMessageRepository extends JpaRepository<PatientChatMessage, Long> {

    boolean existsByMessageKey(String messageKey);

    Optional<PatientChatMessage> findByMessageKey(String messageKey);

    List<PatientChatMessage> findTop20ByPatientPatientIdOrderByMessageTimeDescCreatedAtDesc(String patientId);

    void deleteByPatient_Id(Long patientId);
}
