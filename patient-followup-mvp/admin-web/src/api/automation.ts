import { request } from "./client";
import type { AutomationJob } from "../types";

export async function fetchAutomationJobs(): Promise<AutomationJob[]> {
  const res = await request.get("/api/admin/automation-jobs");
  return res.data;
}
