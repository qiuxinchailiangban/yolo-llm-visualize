package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponse, Long> {

    List<QuestionnaireResponse> findByPatientPatientIdOrderBySubmittedAtDesc(String patientId);

    Optional<QuestionnaireResponse> findFirstByTaskTaskNoOrderBySubmittedAtDesc(String taskNo);

    void deleteByPatientPatientId(String patientId);

    void deleteByPatient_Id(Long patientId);
}
