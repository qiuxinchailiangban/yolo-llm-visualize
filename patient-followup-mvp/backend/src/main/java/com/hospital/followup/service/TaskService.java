package com.hospital.followup.service;

import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.dto.admin.TaskStatusUpdateRequest;
import com.hospital.followup.dto.admin.TaskView;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final QuestionnaireTaskRepository taskRepository;
    private final QuestionnaireService questionnaireService;
    private final TaskReminderService taskReminderService;

    public TaskService(
        QuestionnaireTaskRepository taskRepository,
        QuestionnaireService questionnaireService,
        TaskReminderService taskReminderService
    ) {
        this.taskRepository = taskRepository;
        this.questionnaireService = questionnaireService;
        this.taskReminderService = taskReminderService;
    }

    @Transactional
    public List<TaskView> listTasks(String keyword, QuestionnaireTaskStatus status, Long stageId, LocalDate dueDate) {
        questionnaireService.refreshOverdueTasks();

        return taskRepository.findAll().stream()
            .sorted(Comparator.comparing(QuestionnaireTask::getDueDate).thenComparing(QuestionnaireTask::getCreatedAt).reversed())
            .filter(task -> filterKeyword(task, keyword))
            .filter(task -> status == null || task.getStatus() == status)
            .filter(task -> stageId == null || task.getStage().getId().equals(stageId))
            .filter(task -> dueDate == null || dueDate.equals(task.getDueDate()))
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public TaskView getTask(String taskNo) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        return toView(task);
    }

    @Transactional
    public TaskView updateTaskStatus(String taskNo, TaskStatusUpdateRequest request) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));

        if (request.status() == QuestionnaireTaskStatus.COMPLETED) {
            task.setFinishedAt(LocalDateTime.now());
        } else {
            task.setFinishedAt(null);
        }
        task.setStatus(request.status());
        return toView(task);
    }

    private boolean filterKeyword(QuestionnaireTask task, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(task.getTaskNo(), normalized)
            || contains(task.getPatient().getPatientId(), normalized)
            || contains(task.getPatient().getName(), normalized)
            || contains(task.getStage().getStageName(), normalized);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }

    private TaskView toView(QuestionnaireTask task) {
        return new TaskView(
            task.getTaskNo(),
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            taskReminderService.resolvePreferredConversation(task.getPatient()),
            task.getPatient().getPhone(),
            task.getPatient().getSurgeryDate(),
            task.getStage().getStageCode(),
            task.getStage().getStageName(),
            task.getTemplate().getTemplateName(),
            task.getStatus().name(),
            task.getDueDate(),
            task.getFinishedAt()
        );
    }
}
