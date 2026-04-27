import { request } from "./client";

export interface DailyBatchReminderItem {
  taskNo: string;
  patientId: string;
  patientName: string;
  stageName: string | null;
  dueDate: string | null;
  targetConversation: string | null;
  status:
    | "QUEUED"
    | "SKIPPED_RECENT_REMINDER"
    | "SKIPPED_NO_CONTACT"
    | "FAILED"
    | string;
  message: string | null;
}

export interface DailyBatchReminderResult {
  total: number;
  queued: number;
  skipped: number;
  failed: number;
  items: DailyBatchReminderItem[];
}

export interface DailyBatchReminderPayload {
  countdownSeconds?: number;
  skipRecentlyReminded?: boolean;
  recentWindowHours?: number;
  contentTemplate?: string;
}

export async function sendDailyBatchReminder(
  payload?: DailyBatchReminderPayload
): Promise<DailyBatchReminderResult> {
  const res = await request.post("/api/admin/tasks/reminders/send-today-batch", payload ?? {});
  return res.data;
}
