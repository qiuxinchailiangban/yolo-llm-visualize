package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireTaskRepository extends JpaRepository<QuestionnaireTask, Long> {

    Optional<QuestionnaireTask> findByTaskNo(String taskNo);

    List<QuestionnaireTask> findByPatientPatientIdOrderByDueDateAsc(String patientId);

    List<QuestionnaireTask> findByPatientPatientId(String patientId);

    List<QuestionnaireTask> findByDueDateAndStatusInOrderByDueDateAsc(LocalDate dueDate, Collection<QuestionnaireTaskStatus> statuses);

    List<QuestionnaireTask> findByDueDateLessThanAndStatus(LocalDate dueDate, QuestionnaireTaskStatus status);

    List<QuestionnaireTask> findTop20ByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(LocalDate dueDate, Collection<QuestionnaireTaskStatus> statuses);

    List<QuestionnaireTask> findByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(
        LocalDate dueDate,
        Collection<QuestionnaireTaskStatus> statuses
    );

    void deleteByPatientPatientId(String patientId);

    List<QuestionnaireTask> findByPatient_Id(Long patientId);

    void deleteByPatient_Id(Long patientId);

    List<QuestionnaireTask> findByPatient_IdAndStage_IdAndStatusInOrderByDueDateAsc(
        Long patientId,
        Long stageId,
        Collection<QuestionnaireTaskStatus> statuses
    );
}
