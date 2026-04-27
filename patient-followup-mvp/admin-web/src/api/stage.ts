import { request } from "./client";
import type { Stage, StageForm } from "../types";

export async function fetchStages(): Promise<Stage[]> {
  const res = await request.get("/api/admin/stages");
  return res.data;
}

export async function createStage(payload: StageForm): Promise<Stage> {
  const res = await request.post("/api/admin/stages", payload);
  return res.data;
}

export async function updateStage(id: number, payload: StageForm): Promise<Stage> {
  const res = await request.put(`/api/admin/stages/${id}`, payload);
  return res.data;
}
