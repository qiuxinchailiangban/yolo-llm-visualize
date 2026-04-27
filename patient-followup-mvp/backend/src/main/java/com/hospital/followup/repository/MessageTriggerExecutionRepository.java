package com.hospital.followup.repository;

import com.hospital.followup.domain.MessageTriggerExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTriggerExecutionRepository extends JpaRepository<MessageTriggerExecution, Long> {

    boolean existsByTriggerKey(String triggerKey);

    Optional<MessageTriggerExecution> findByAutomationJobNo(String automationJobNo);

    List<MessageTriggerExecution> findByPatient_Id(Long patientId);

    void deleteByPatient_Id(Long patientId);
}
