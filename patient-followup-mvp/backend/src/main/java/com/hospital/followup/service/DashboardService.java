package com.hospital.followup.service;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.dto.admin.DashboardTodoItem;
import com.hospital.followup.dto.admin.DashboardView;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;
    private final QuestionnaireTaskRepository taskRepository;
    private final QuestionnaireService questionnaireService;

    public DashboardService(
        PatientRepository patientRepository,
        QuestionnaireTaskRepository taskRepository,
        QuestionnaireService questionnaireService
    ) {
        this.patientRepository = patientRepository;
        this.taskRepository = taskRepository;
        this.questionnaireService = questionnaireService;
    }

    @Transactional
    public DashboardView getDashboard() {
        questionnaireService.refreshOverdueTasks();
        LocalDate today = LocalDate.now();

        List<Patient> surgeriesToday = patientRepository.findBySurgeryDateOrderByCreatedAtDesc(today);
        List<Patient> allPatients = patientRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .toList();
        List<QuestionnaireTask> dueToday = taskRepository.findByDueDateAndStatusInOrderByDueDateAsc(
            today,
            Set.of(QuestionnaireTaskStatus.PENDING, QuestionnaireTaskStatus.OVERDUE)
        );
        List<QuestionnaireTask> remindable = taskRepository.findTop20ByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(
            today,
            Set.of(QuestionnaireTaskStatus.PENDING, QuestionnaireTaskStatus.OVERDUE)
        );

        return new DashboardView(
            surgeriesToday.size(),
            dueToday.size(),
            remindable.size(),
            surgeriesToday.stream().map(this::mapPatientTodo).toList(),
            dueToday.stream().map(this::mapTaskTodo).toList(),
            remindable.stream().map(this::mapTaskTodo).toList(),
            allPatients.stream().map(this::mapPatientTodo).toList()
        );
    }

    private DashboardTodoItem mapPatientTodo(Patient patient) {
        return new DashboardTodoItem(
            patient.getPatientId(),
            patient.getName(),
            null,
            null,
            patient.getSurgeryDate() == null ? null : patient.getSurgeryDate().toString(),
            patient.getDiagnosis()
        );
    }

    private DashboardTodoItem mapTaskTodo(QuestionnaireTask task) {
        return new DashboardTodoItem(
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            task.getTaskNo(),
            task.getStage().getStageName(),
            task.getDueDate().toString(),
            task.getStatus().name()
        );
    }
}
