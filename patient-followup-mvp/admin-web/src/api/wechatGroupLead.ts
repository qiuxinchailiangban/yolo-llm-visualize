import { request } from "./client";
import type { WechatGroupLead } from "../types";

export async function fetchWechatGroupLeads(): Promise<WechatGroupLead[]> {
  const res = await request.get("/api/admin/wechat-group-leads");
  return res.data;
}

export async function createPatientFromWechatGroupLead(chatroomUsername: string): Promise<WechatGroupLead> {
  const res = await request.post(`/api/admin/wechat-group-leads/${encodeURIComponent(chatroomUsername)}/create-patient`);
  return res.data;
}

export async function bindPatientToWechatGroupLead(chatroomUsername: string, patientId: string): Promise<WechatGroupLead> {
  const res = await request.post(`/api/admin/wechat-group-leads/${encodeURIComponent(chatroomUsername)}/bind-patient`, {
    patientId,
  });
  return res.data;
}

export async function unbindPatientFromWechatGroupLead(chatroomUsername: string): Promise<WechatGroupLead> {
  const res = await request.delete(`/api/admin/wechat-group-leads/${encodeURIComponent(chatroomUsername)}/bind-patient`);
  return res.data;
}
