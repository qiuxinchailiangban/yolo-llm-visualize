import { request } from "./client";

export interface SelfCheckItem {
  key: string;
  name: string;
  level: "OK" | "WARN" | "ERROR";
  ok: boolean;
  message: string;
  hint?: string | null;
}

export interface SelfCheckResult {
  overallOk: boolean;
  items: SelfCheckItem[];
}

export async function runSelfCheck(): Promise<SelfCheckResult> {
  const res = await request.get("/api/admin/self-check");
  return res.data;
}
