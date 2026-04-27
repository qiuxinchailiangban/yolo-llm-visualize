import { request } from "./client";
import type {
  MessageTriggerManualCandidate,
  MessageTriggerManualExecuteItem,
  MessageTriggerManualExecuteResult,
  MessageTriggerRule,
  MessageTriggerRuleMediaUploadResult,
} from "../types";

export async function fetchMessageTriggerRules(): Promise<MessageTriggerRule[]> {
  const res = await request.get("/api/admin/message-trigger-rules");
  return res.data;
}

export async function fetchManualMessageTriggerRules(): Promise<MessageTriggerRule[]> {
  const res = await request.get("/api/admin/message-trigger-rules/manual-rules");
  return res.data;
}

export async function createMessageTriggerRule(payload: MessageTriggerRule): Promise<MessageTriggerRule> {
  const res = await request.post("/api/admin/message-trigger-rules", payload);
  return res.data;
}

export async function updateMessageTriggerRule(id: number, payload: MessageTriggerRule): Promise<MessageTriggerRule> {
  const res = await request.put(`/api/admin/message-trigger-rules/${id}`, payload);
  return res.data;
}

export async function deleteMessageTriggerRule(id: number): Promise<void> {
  await request.delete(`/api/admin/message-trigger-rules/${id}`);
}

export async function uploadMessageTriggerRuleMedia(file: File): Promise<MessageTriggerRuleMediaUploadResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await request.post("/api/admin/message-trigger-rules/upload-media", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return res.data;
}

export async function detectManualMessageTriggerCandidates(payload: {
  ruleIds?: number[];
  patientIds?: string[];
}): Promise<MessageTriggerManualCandidate[]> {
  const res = await request.post("/api/admin/message-trigger-rules/manual-detect", payload);
  return res.data;
}

export async function executeManualMessageTriggerCandidates(
  items: MessageTriggerManualExecuteItem[],
): Promise<MessageTriggerManualExecuteResult> {
  const res = await request.post("/api/admin/message-trigger-rules/manual-execute", { items });
  return res.data;
}
