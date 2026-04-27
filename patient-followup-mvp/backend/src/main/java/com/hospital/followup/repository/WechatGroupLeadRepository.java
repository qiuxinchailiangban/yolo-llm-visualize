package com.hospital.followup.repository;

import com.hospital.followup.domain.WechatGroupLead;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WechatGroupLeadRepository extends JpaRepository<WechatGroupLead, Long> {

    Optional<WechatGroupLead> findByChatroomUsername(String chatroomUsername);

    List<WechatGroupLead> findAllByOrderByUpdatedAtDesc();

    List<WechatGroupLead> findByParseStatusOrderByUpdatedAtDesc(String parseStatus);
}
