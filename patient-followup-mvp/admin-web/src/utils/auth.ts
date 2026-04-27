import type { AdminUser, LoginResult } from "../types";

const TOKEN_KEY = "patient-followup-token";
const USER_KEY = "patient-followup-user";

export function saveAuth(result: LoginResult) {
  localStorage.setItem(TOKEN_KEY, result.token);
  localStorage.setItem(USER_KEY, JSON.stringify(result.user));
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function getUser(): AdminUser | null {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as AdminUser) : null;
}

export function isLoggedIn() {
  return Boolean(getToken());
}
