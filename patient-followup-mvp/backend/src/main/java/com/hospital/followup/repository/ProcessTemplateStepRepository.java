package com.hospital.followup.repository;

import com.hospital.followup.domain.ProcessTemplateStep;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessTemplateStepRepository extends JpaRepository<ProcessTemplateStep, Long> {

    List<ProcessTemplateStep> findByTemplate_IdOrderBySortOrderAsc(Long templateId);

    List<ProcessTemplateStep> findByTemplate_TemplateCodeOrderBySortOrderAsc(String templateCode);

    Optional<ProcessTemplateStep> findByTemplate_IdAndStepCode(Long templateId, String stepCode);
}
