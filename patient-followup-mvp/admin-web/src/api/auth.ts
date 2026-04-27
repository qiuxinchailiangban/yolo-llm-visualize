import { request } from "./client";
import type { AdminUser, LoginResult } from "../types";

export async function login(username: string, password: string): Promise<LoginResult> {
  const res = await request.post("/api/admin/auth/login", { username, password });
  return res.data;
}

export async function fetchCurrentUser(): Promise<AdminUser> {
  const res = await request.get("/api/admin/auth/me");
  return res.data;
}

export async function logout(): Promise<void> {
  await request.post("/api/admin/auth/logout");
}
