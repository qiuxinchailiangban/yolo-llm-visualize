import { request } from "./client";
import type { ReminderTaskLog, TaskItem, TaskReminderSendResult } from "../types";

interface TaskQuery {
  keyword?: string;
  status?: string;
  stageId?: number;
  dueDate?: string;
}

export async function fetchTasks(params: TaskQuery): Promise<TaskItem[]> {
  const res = await request.get("/api/admin/tasks", { params });
  return res.data;
}

export async function updateTaskStatus(taskNo: string, status: string): Promise<TaskItem> {
  const res = await request.patch(`/api/admin/tasks/${taskNo}/status`, { status });
  return res.data;
}

export async function sendTaskReminder(
  taskNo: string,
  payload: { targetConversation: string; content: string; countdownSeconds?: number },
): Promise<TaskReminderSendResult> {
  const res = await request.post(`/api/admin/tasks/${taskNo}/send-reminder`, payload);
  return res.data;
}

export async function fetchTaskReminders(taskNo: string): Promise<ReminderTaskLog[]> {
  const res = await request.get(`/api/admin/tasks/${taskNo}/reminders`);
  return res.data;
}
