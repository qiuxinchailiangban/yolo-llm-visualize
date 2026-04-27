import { request } from "./client";
import type { Patient, PatientDetail, PatientForm, PatientImportResult, PatientTaskRebuildResult } from "../types";

export async function fetchPatients(keyword = ""): Promise<Patient[]> {
  const res = await request.get("/api/admin/patients", { params: { keyword } });
  return res.data;
}

export async function fetchPatientDetail(patientId: string): Promise<PatientDetail> {
  const res = await request.get(`/api/admin/patients/${patientId}`);
  return res.data;
}

export async function createPatient(payload: PatientForm): Promise<Patient> {
  const res = await request.post("/api/admin/patients", payload);
  return res.data;
}

export async function updatePatient(patientId: string, payload: PatientForm): Promise<Patient> {
  const res = await request.put(`/api/admin/patients/${patientId}`, payload);
  return res.data;
}

export async function deletePatient(patientId: string): Promise<void> {
  await request.delete(`/api/admin/patients/${patientId}`);
}

export async function importPatientCsv(csvContent: string): Promise<PatientImportResult> {
  const res = await request.post("/api/admin/patients/import-csv", { csvContent });
  return res.data;
}

export async function rebuildAllPatientTasks(): Promise<PatientTaskRebuildResult> {
  const res = await request.post("/api/admin/patients/rebuild-tasks");
  return res.data;
}
