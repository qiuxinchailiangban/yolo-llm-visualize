import { request } from "./client";
import type { DashboardData } from "../types";

export async function fetchDashboard(): Promise<DashboardData> {
  const res = await request.get("/api/admin/dashboard/todos");
  return res.data;
}
