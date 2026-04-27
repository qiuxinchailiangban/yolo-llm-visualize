package com.hospital.followup.repository;

import com.hospital.followup.domain.FollowupStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowupStageRepository extends JpaRepository<FollowupStage, Long> {

    List<FollowupStage> findByEnabledTrueOrderBySortOrderAsc();

    Optional<FollowupStage> findByStageCode(String stageCode);
}
