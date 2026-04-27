package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireTemplateRepository extends JpaRepository<QuestionnaireTemplate, Long> {

    List<QuestionnaireTemplate> findByStatusOrderByUpdatedAtDesc(TemplateStatus status);

    List<QuestionnaireTemplate> findByTemplateNameContainingIgnoreCaseOrTemplateCodeContainingIgnoreCaseOrderByUpdatedAtDesc(String name, String code);

    Optional<QuestionnaireTemplate> findByTemplateCode(String templateCode);

    Optional<QuestionnaireTemplate> findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType templateType, TemplateStatus status);

    Optional<QuestionnaireTemplate> findFirstByStageIdAndStatusOrderByUpdatedAtDesc(Long stageId, TemplateStatus status);
}
