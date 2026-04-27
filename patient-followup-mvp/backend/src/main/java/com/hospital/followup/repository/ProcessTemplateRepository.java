package com.hospital.followup.repository;

import com.hospital.followup.domain.ProcessTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessTemplateRepository extends JpaRepository<ProcessTemplate, Long> {

    Optional<ProcessTemplate> findByTemplateCode(String templateCode);

    Optional<ProcessTemplate> findFirstByDefaultTemplateTrueAndActiveTrueOrderByUpdatedAtDesc();

    List<ProcessTemplate> findAllByOrderByDefaultTemplateDescActiveDescUpdatedAtDesc();
}
