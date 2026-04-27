package com.hospital.followup.repository;

import com.hospital.followup.domain.MessageTriggerRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTriggerRuleRepository extends JpaRepository<MessageTriggerRule, Long> {

    List<MessageTriggerRule> findAllByOrderBySortOrderAscCreatedAtDesc();

    List<MessageTriggerRule> findByEnabledTrueOrderBySortOrderAscCreatedAtAsc();

    Optional<MessageTriggerRule> findByRuleCode(String ruleCode);
}
