<template>
  <div class="page exception-page">
    <div class="hero">
      <div>
        <div class="hero-title">流程异常中心</div>
        <div class="hero-subtitle">集中查看卡住、发送失败、待反馈超时的患者流程节点，优先处理高风险病例</div>
      </div>
      <div class="hero-actions">
        <el-button @click="goDashboard">返回患者流程</el-button>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">异常总数</div>
          <div class="metric-value">{{ data?.total ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">发送失败</div>
          <div class="metric-value danger">{{ data?.sendFailureCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">待反馈超时</div>
          <div class="metric-value warning">{{ data?.feedbackTimeoutCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">其他异常</div>
          <div class="metric-value">{{ data?.warningCount ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <div class="section-title">异常列表</div>
          <el-tag type="danger">按最近更新时间排序</el-tag>
        </div>
      </template>

      <el-table :data="data?.items ?? []" @row-click="openPatientProcess" row-class-name="clickable-row">
        <el-table-column prop="patientName" label="患者" width="120" />
        <el-table-column prop="patientId" label="Patient ID" width="170" />
        <el-table-column prop="surgeryDate" label="手术日期" width="120" />
        <el-table-column prop="surgeryScheduleTag" label="手术场次" width="120" />
        <el-table-column prop="stepName" label="异常节点" min-width="180" />
        <el-table-column label="异常类型" width="130">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.exceptionType)">{{ typeLabel(row.exceptionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="异常原因" min-width="260" show-overflow-tooltip />
        <el-table-column prop="linkedQuestionnaireTaskNo" label="问卷任务" width="140" />
        <el-table-column prop="linkedAutomationJobNo" label="自动化任务" width="140" />
        <el-table-column prop="updatedAt" label="最近更新" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchPatientProcessExceptions } from "../api/patientProcess";
import type { PatientProcessExceptionCenter, PatientProcessExceptionItem } from "../types";

const router = useRouter();
const data = ref<PatientProcessExceptionCenter>();

async function loadData() {
  data.value = await fetchPatientProcessExceptions();
}

function goDashboard() {
  router.push("/patient-processes");
}

function openPatientProcess(row: PatientProcessExceptionItem) {
  router.push("/patient-processes");
  window.setTimeout(() => {
    window.dispatchEvent(new CustomEvent("patient-process:open", { detail: { patientId: row.patientId } }));
  }, 80);
}

function typeLabel(type?: string) {
  if (type === "SEND_FAILURE") return "发送失败";
  if (type === "FEEDBACK_TIMEOUT") return "反馈超时";
  if (type === "STEP_WARNING") return "流程异常";
  return type || "-";
}

function typeTag(type?: string): "danger" | "warning" | "info" {
  if (type === "SEND_FAILURE") return "danger";
  if (type === "FEEDBACK_TIMEOUT") return "warning";
  return "info";
}

onMounted(loadData);
</script>

<style scoped>
.exception-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #450a0a 0%, #991b1b 50%, #ef4444 100%);
  color: #fff;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
}

.hero-subtitle {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.86);
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.metric-card {
  border-radius: 18px;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 14px;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
}

.metric-value.danger {
  color: #dc2626;
}

.metric-value.warning {
  color: #d97706;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

:deep(.clickable-row) {
  cursor: pointer;
}
</style>
