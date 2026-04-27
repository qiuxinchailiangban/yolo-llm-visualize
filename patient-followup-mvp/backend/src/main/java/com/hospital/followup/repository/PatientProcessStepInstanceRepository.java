package com.hospital.followup.repository;

import com.hospital.followup.domain.PatientProcessStepInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProcessStepInstanceRepository extends JpaRepository<PatientProcessStepInstance, Long> {

    List<PatientProcessStepInstance> findByInstance_IdOrderBySortOrderAsc(Long instanceId);

    Optional<PatientProcessStepInstance> findByInstance_IdAndStepCode(Long instanceId, String stepCode);

    void deleteByInstance_Id(Long instanceId);
}
