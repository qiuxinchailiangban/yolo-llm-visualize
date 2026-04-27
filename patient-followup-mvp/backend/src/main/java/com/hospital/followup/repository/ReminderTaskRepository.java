package com.hospital.followup.repository;

import com.hospital.followup.domain.ReminderTask;
import com.hospital.followup.domain.enums.ReminderTaskStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderTaskRepository extends JpaRepository<ReminderTask, Long> {
    List<ReminderTask> findByQuestionnaireTaskTaskNoOrderByCreatedAtDesc(String taskNo);

    List<ReminderTask> findByQuestionnaireTaskIdIn(Collection<Long> taskIds);

    List<ReminderTask> findByQuestionnaireTaskIdInAndStatusInAndCreatedAtAfter(
        Collection<Long> taskIds,
        Collection<ReminderTaskStatus> statuses,
        LocalDateTime after
    );

    void deleteByQuestionnaireTaskIdIn(Collection<Long> taskIds);
}
