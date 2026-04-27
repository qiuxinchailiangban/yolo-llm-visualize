import { request } from "./client";
import type { PatientProcessDashboard, PatientProcessDetail, PatientProcessExceptionCenter, PatientProcessTemplate } from "../types";

export async function fetchPatientProcessDashboard(keyword = ""): Promise<PatientProcessDashboard> {
  const res = await request.get("/api/admin/patient-processes/dashboard", { params: { keyword } });
  return res.data;
}

export async function fetchPatientProcessDetail(patientId: string): Promise<PatientProcessDetail> {
  const res = await request.get(`/api/admin/patient-processes/${patientId}`);
  return res.data;
}

export async function syncPatientProcess(patientId: string): Promise<PatientProcessDetail> {
  const res = await request.post(`/api/admin/patient-processes/${patientId}/sync`);
  return res.data;
}

export async function fetchPatientProcessTemplates(): Promise<PatientProcessTemplate[]> {
  const res = await request.get("/api/admin/patient-processes/templates");
  return res.data;
}

export async function fetchPatientProcessExceptions(): Promise<PatientProcessExceptionCenter> {
  const res = await request.get("/api/admin/patient-processes/exceptions");
  return res.data;
}

export async function createPatientProcessTemplate(payload: PatientProcessTemplate): Promise<PatientProcessTemplate> {
  const res = await request.post("/api/admin/patient-processes/templates", payload);
  return res.data;
}

export async function updatePatientProcessTemplate(id: number, payload: PatientProcessTemplate): Promise<PatientProcessTemplate> {
  const res = await request.put(`/api/admin/patient-processes/templates/${id}`, payload);
  return res.data;
}
