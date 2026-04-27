package com.hospital.followup.repository;

import com.hospital.followup.domain.AutomationJob;
import com.hospital.followup.domain.enums.AutomationJobStatus;
import com.hospital.followup.domain.enums.AutomationJobType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationJobRepository extends JpaRepository<AutomationJob, Long> {

    Optional<AutomationJob> findByJobNo(String jobNo);

    Optional<AutomationJob> findFirstByStatusAndJobTypeAndPlannedAtLessThanEqualOrderByPlannedAtAscCreatedAtAsc(
        AutomationJobStatus status,
        AutomationJobType jobType,
        LocalDateTime plannedAt
    );

    List<AutomationJob> findTop100ByOrderByCreatedAtDesc();

    List<AutomationJob> findByBizTypeAndBizIdIn(String bizType, Collection<Long> bizIds);
}
