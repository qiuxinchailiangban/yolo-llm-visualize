import { request } from "./client";
import type { Template } from "../types";

export async function fetchTemplates(): Promise<Template[]> {
  const res = await request.get("/api/admin/templates");
  return res.data;
}

export async function createTemplate(payload: Template): Promise<Template> {
  const res = await request.post("/api/admin/templates", payload);
  return res.data;
}

export async function updateTemplate(id: number, payload: Template): Promise<Template> {
  const res = await request.put(`/api/admin/templates/${id}`, payload);
  return res.data;
}
