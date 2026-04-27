package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.dto.admin.DailyBatchReminderRequest;
import com.hospital.followup.dto.admin.DailyBatchReminderResult;
import com.hospital.followup.dto.admin.ReminderTaskView;
import com.hospital.followup.dto.admin.TaskReminderSendRequest;
import com.hospital.followup.dto.admin.TaskReminderSendResult;
import com.hospital.followup.dto.admin.TaskStatusUpdateRequest;
import com.hospital.followup.dto.admin.TaskView;
import com.hospital.followup.service.TaskReminderService;
import com.hospital.followup.service.TaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private final TaskService taskService;
    private final TaskReminderService taskReminderService;

    public AdminTaskController(TaskService taskService, TaskReminderService taskReminderService) {
        this.taskService = taskService;
        this.taskReminderService = taskReminderService;
    }

    @GetMapping
    public ApiResponse<List<TaskView>> listTasks(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) QuestionnaireTaskStatus status,
        @RequestParam(required = false) Long stageId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) {
        return ApiResponse.ok(taskService.listTasks(keyword, status, stageId, dueDate));
    }

    @GetMapping("/{taskNo}")
    public ApiResponse<TaskView> getTask(@PathVariable String taskNo) {
        return ApiResponse.ok(taskService.getTask(taskNo));
    }

    @GetMapping("/{taskNo}/reminders")
    public ApiResponse<List<ReminderTaskView>> listReminders(@PathVariable String taskNo) {
        return ApiResponse.ok(taskReminderService.listTaskReminders(taskNo));
    }

    @PatchMapping("/{taskNo}/status")
    public ApiResponse<TaskView> updateTaskStatus(@PathVariable String taskNo, @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ApiResponse.ok(taskService.updateTaskStatus(taskNo, request), "任务状态更新成功");
    }

    @PostMapping("/{taskNo}/send-reminder")
    public ApiResponse<TaskReminderSendResult> sendReminder(
        @PathVariable String taskNo,
        @Valid @RequestBody TaskReminderSendRequest request
    ) {
        TaskReminderSendResult result = taskReminderService.sendManualReminder(taskNo, request);
        String message = "QUEUED".equals(result.status()) ? "提醒任务已加入执行队列" : result.message();
        return ApiResponse.ok(result, message);
    }

    @PostMapping("/reminders/send-today-batch")
    public ApiResponse<DailyBatchReminderResult> sendDailyBatchReminder(
        @Valid @RequestBody(required = false) DailyBatchReminderRequest request
    ) {
        DailyBatchReminderRequest effective = request == null
            ? new DailyBatchReminderRequest(null, null, null, null)
            : request;
        DailyBatchReminderResult result = taskReminderService.sendDailyBatch(effective);
        String message = "本次共处理 " + result.total()
            + " 个任务：入队 " + result.queued()
            + "、跳过 " + result.skipped()
            + "、失败 " + result.failed();
        return ApiResponse.ok(result, message);
    }
}
