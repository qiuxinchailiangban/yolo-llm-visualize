package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireQrCode;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireQrCodeRepository extends JpaRepository<QuestionnaireQrCode, Long> {

    Optional<QuestionnaireQrCode> findByToken(String token);

    void deleteByTask_IdIn(Collection<Long> taskIds);
}
