<template>
  <div class="page process-page">
    <div class="hero">
      <div>
        <div class="hero-title">患者流程总览</div>
        <div class="hero-subtitle">围手术期每位患者当前进度、待反馈节点和异常节点一屏查看</div>
      </div>
      <div class="hero-actions">
        <el-input v-model="keyword" placeholder="按患者姓名或 Patient ID 搜索" clearable @keyup.enter="loadDashboard" />
        <el-button @click="loadDashboard">刷新</el-button>
        <el-button type="warning" @click="goExceptions">异常中心</el-button>
        <el-button type="primary" @click="goTemplates">流程模板</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">患者流程数</div>
          <div class="metric-value">{{ dashboard?.activeInstances ?? 0 }}</div>
          <div class="metric-note">已自动生成流程实例</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">待反馈患者</div>
          <div class="metric-value warning">{{ dashboard?.waitingFeedbackPatients ?? 0 }}</div>
          <div class="metric-note">已发送但仍在等待患者反馈</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">异常患者</div>
          <div class="metric-value danger">{{ dashboard?.warningPatients ?? 0 }}</div>
          <div class="metric-note">存在发送失败或逾期未完成节点</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">患者总数</div>
          <div class="metric-value">{{ dashboard?.totalPatients ?? 0 }}</div>
          <div class="metric-note">当前纳入流程追踪的患者</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="overview-card" shadow="never">
      <template #header>
        <div class="section-header">
          <div>
            <div class="section-title">流程实例列表</div>
            <div class="section-subtitle">点击任一患者可查看完整流程时间轴和节点明细</div>
          </div>
          <el-tag type="info">共 {{ dashboard?.items.length ?? 0 }} 条</el-tag>
        </div>
      </template>

      <el-table :data="dashboard?.items ?? []" @row-click="openDetail" row-class-name="clickable-row">
        <el-table-column prop="patientName" label="患者" width="120" />
        <el-table-column prop="patientId" label="Patient ID" width="170" />
        <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip />
        <el-table-column prop="surgeryDate" label="手术日期" width="120" />
        <el-table-column prop="templateName" label="流程模板" width="150" />
        <el-table-column label="当前节点" min-width="200">
          <template #default="{ row }">
            <div class="current-step">
              <div class="current-step-name">{{ row.currentStepName || "流程已完成" }}</div>
              <div class="current-step-summary">{{ row.summaryText || "-" }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.progressPercent" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待反馈" width="90">
          <template #default="{ row }">
            <el-badge :value="row.waitingFeedbackCount" :hidden="!row.waitingFeedbackCount" type="warning">
              <span>{{ row.waitingFeedbackCount }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="异常" width="90">
          <template #default="{ row }">
            <el-badge :value="row.warningStepCount" :hidden="!row.warningStepCount" type="danger">
              <span>{{ row.warningStepCount }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新" width="180" />
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" size="72%" :title="detailTitle" destroy-on-close>
      <template v-if="detail">
        <div class="detail-shell">
          <div class="detail-top">
            <el-card shadow="never" class="profile-card">
              <div class="profile-head">
                <div>
                  <div class="profile-name">{{ detail.patient.name }}</div>
                  <div class="profile-meta">
                    <span>{{ detail.patient.patientId }}</span>
                    <span>{{ detail.patient.surgeryDate || "未填手术日期" }}</span>
                    <span>{{ detail.patient.wechatGroupName || "未绑定群" }}</span>
                  </div>
                </div>
                <div class="profile-actions">
                  <el-button :loading="syncing" @click="syncDetail">同步流程</el-button>
                </div>
              </div>
              <div class="summary-grid">
                <div class="summary-box">
                  <span>流程状态</span>
                  <strong>{{ statusLabel(detail.status) }}</strong>
                </div>
                <div class="summary-box">
                  <span>当前节点</span>
                  <strong>{{ detail.currentStepName || "流程已完成" }}</strong>
                </div>
                <div class="summary-box">
                  <span>完成进度</span>
                  <strong>{{ detail.progressPercent }}%</strong>
                </div>
                <div class="summary-box">
                  <span>待反馈</span>
                  <strong>{{ detail.waitingFeedbackCount }}</strong>
                </div>
              </div>
              <div class="detail-summary">{{ detail.summaryText || "当前暂无摘要" }}</div>
            </el-card>
          </div>

          <div class="timeline-layout">
            <div class="timeline-column">
              <div v-for="step in detail.steps" :key="step.id" class="timeline-item" :class="statusClass(step.status)">
                <div class="timeline-marker">
                  <div class="timeline-dot"></div>
                  <div class="timeline-line"></div>
                </div>
                <div class="timeline-card">
                  <div class="timeline-card-head">
                    <div>
                      <div class="timeline-title">{{ step.stepName }}</div>
                      <div class="timeline-subtitle">
                        {{ stepTypeLabel(step.stepType) }} · {{ triggerLabel(step) }}
                      </div>
                    </div>
                    <el-tag :type="statusTagType(step.status)">{{ statusLabel(step.status) }}</el-tag>
                  </div>
                  <div class="timeline-body">
                    <div class="timeline-meta">
                      <span>计划日期：{{ step.plannedDate || "-" }}</span>
                      <span>触发时间：{{ step.triggeredAt || "-" }}</span>
                      <span>完成时间：{{ step.completedAt || "-" }}</span>
                    </div>
                    <div v-if="step.statusReason" class="timeline-reason">{{ step.statusReason }}</div>
                    <div v-if="step.feedbackSummary" class="timeline-feedback">{{ step.feedbackSummary }}</div>
                    <div class="timeline-links">
                      <span v-if="step.linkedQuestionnaireTaskNo">问卷任务：{{ step.linkedQuestionnaireTaskNo }}</span>
                      <span v-if="step.linkedAutomationJobNo">自动化任务：{{ step.linkedAutomationJobNo }}</span>
                      <span v-if="step.linkedMessageRuleCode">关联任务：{{ step.linkedMessageRuleCode }}</span>
                      <span v-if="step.applicableSurgeryTags">适用场次：{{ step.applicableSurgeryTags }}</span>
                    </div>
                    <div v-if="step.linkedQuestionnaireTaskNo || step.linkedAutomationJobNo" class="timeline-panels">
                      <div v-if="step.linkedQuestionnaireTaskNo" class="detail-panel">
                        <div class="detail-panel-title">问卷记录</div>
                        <div class="detail-panel-item">任务号：{{ step.linkedQuestionnaireTaskNo }}</div>
                        <div class="detail-panel-item">任务状态：{{ step.linkedQuestionnaireStatus || "-" }}</div>
                        <div class="detail-panel-item">应填日期：{{ step.linkedQuestionnaireDueDate || "-" }}</div>
                        <div class="detail-panel-item">完成时间：{{ step.linkedQuestionnaireFinishedAt || "-" }}</div>
                        <div class="detail-panel-item">提交时间：{{ step.linkedQuestionnaireResponseSubmittedAt || "-" }}</div>
                        <div v-if="step.linkedQuestionnaireResponsePreview" class="detail-panel-pre">
                          {{ step.linkedQuestionnaireResponsePreview }}
                        </div>
                      </div>
                      <div v-if="step.linkedAutomationJobNo" class="detail-panel">
                        <div class="detail-panel-title">自动化任务日志</div>
                        <div class="detail-panel-item">任务号：{{ step.linkedAutomationJobNo }}</div>
                        <div class="detail-panel-item">任务状态：{{ step.linkedAutomationJobStatus || "-" }}</div>
                        <div v-if="step.linkedAutomationJobLastError" class="detail-panel-item error">
                          错误：{{ step.linkedAutomationJobLastError }}
                        </div>
                        <pre v-if="step.linkedAutomationJobExecutionLog" class="detail-panel-pre">{{ step.linkedAutomationJobExecutionLog }}</pre>
                      </div>
                    </div>
                    <div v-if="step.displayHint" class="timeline-hint">{{ step.displayHint }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { fetchPatientProcessDashboard, fetchPatientProcessDetail, syncPatientProcess } from "../api/patientProcess";
import type { PatientProcessDashboard, PatientProcessDetail, PatientProcessOverview, PatientProcessStep } from "../types";

const router = useRouter();
const keyword = ref("");
const dashboard = ref<PatientProcessDashboard>();
const detail = ref<PatientProcessDetail>();
const detailVisible = ref(false);
const syncing = ref(false);

const detailTitle = computed(() => (detail.value ? `${detail.value.patient.name} · 患者流程` : "患者流程详情"));

async function loadDashboard() {
  dashboard.value = await fetchPatientProcessDashboard(keyword.value);
}

async function openDetail(row: PatientProcessOverview) {
  detail.value = await fetchPatientProcessDetail(row.patientId);
  detailVisible.value = true;
}

async function openDetailByPatientId(patientId: string) {
  detail.value = await fetchPatientProcessDetail(patientId);
  detailVisible.value = true;
}

async function syncDetail() {
  if (!detail.value) {
    return;
  }
  syncing.value = true;
  try {
    detail.value = await syncPatientProcess(detail.value.patient.patientId);
    await loadDashboard();
    ElMessage.success("流程已同步");
  } finally {
    syncing.value = false;
  }
}

function goTemplates() {
  router.push("/patient-process-templates");
}

function goExceptions() {
  router.push("/patient-process-exceptions");
}

function statusLabel(status?: string) {
  if (status === "ACTIVE") return "运行中";
  if (status === "COMPLETED") return "已完成";
  if (status === "WARNING") return "需关注";
  if (status === "UPCOMING") return "未开始";
  if (status === "READY") return "待触发";
  if (status === "RUNNING") return "执行中";
  if (status === "WAITING_FEEDBACK") return "待反馈";
  if (status === "SKIPPED") return "已跳过";
  return status || "-";
}

function statusTagType(status?: string): "success" | "warning" | "danger" | "info" {
  if (status === "COMPLETED") return "success";
  if (status === "WARNING") return "danger";
  if (status === "WAITING_FEEDBACK" || status === "RUNNING") return "warning";
  return "info";
}

function stepTypeLabel(stepType?: string) {
  return stepType === "QUESTIONNAIRE" ? "问卷节点" : "消息节点";
}

function triggerLabel(step: PatientProcessStep) {
  if (step.triggerMode === "EVENT_BIND_GROUP") {
    return "绑定群后触发";
  }
  const offset = step.relativeDayOffset ?? 0;
  if (offset === 0) {
    return "手术当天";
  }
  if (offset > 0) {
    return `术后 ${offset} 天`;
  }
  return `术前 ${Math.abs(offset)} 天`;
}

function statusClass(status?: string) {
  return `status-${(status || "UNKNOWN").toLowerCase()}`;
}

function handleExternalOpen(event: Event) {
  const customEvent = event as CustomEvent<{ patientId?: string }>;
  const patientId = customEvent.detail?.patientId;
  if (patientId) {
    openDetailByPatientId(patientId);
  }
}

onMounted(() => {
  loadDashboard();
  window.addEventListener("patient-process:open", handleExternalOpen as EventListener);
});

onBeforeUnmount(() => {
  window.removeEventListener("patient-process:open", handleExternalOpen as EventListener);
});
</script>

<style scoped>
.process-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 55%, #60a5fa 100%);
  color: #fff;
  box-shadow: 0 20px 45px rgba(29, 78, 216, 0.18);
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
}

.hero-subtitle {
  margin-top: 8px;
  max-width: 620px;
  color: rgba(255, 255, 255, 0.82);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-actions :deep(.el-input) {
  width: 280px;
}

.metric-row,
.overview-card {
  margin-top: 0;
}

.metric-card {
  border: none;
  border-radius: 20px;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 14px;
  font-size: 34px;
  font-weight: 700;
  color: #0f172a;
}

.metric-value.warning {
  color: #d97706;
}

.metric-value.danger {
  color: #dc2626;
}

.metric-note {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
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

.section-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.current-step-name {
  font-weight: 600;
  color: #0f172a;
}

.current-step-summary {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  border-radius: 24px;
}

.profile-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: #64748b;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.summary-box {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.summary-box span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.summary-box strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 18px;
}

.detail-summary {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eff6ff;
  color: #1d4ed8;
}

.timeline-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-dot {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #94a3b8;
  border: 4px solid #e2e8f0;
}

.timeline-line {
  flex: 1;
  width: 2px;
  margin-top: 8px;
  background: #e2e8f0;
}

.timeline-item:last-child .timeline-line {
  display: none;
}

.timeline-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.timeline-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.timeline-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.timeline-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.timeline-body {
  margin-top: 14px;
}

.timeline-meta,
.timeline-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.timeline-reason {
  margin-top: 12px;
  color: #0f172a;
  font-weight: 600;
}

.timeline-feedback {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  color: #334155;
}

.timeline-links {
  margin-top: 12px;
}

.timeline-hint {
  margin-top: 12px;
  color: #64748b;
  line-height: 1.6;
}

.timeline-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.detail-panel {
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.detail-panel-title {
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
}

.detail-panel-item {
  color: #475569;
  font-size: 12px;
  line-height: 1.8;
}

.detail-panel-item.error {
  color: #dc2626;
}

.detail-panel-pre {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
  max-height: 220px;
  overflow: auto;
}

.status-completed .timeline-dot {
  background: #16a34a;
}

.status-warning .timeline-dot {
  background: #dc2626;
}

.status-waiting_feedback .timeline-dot,
.status-running .timeline-dot {
  background: #d97706;
}

.status-ready .timeline-dot {
  background: #2563eb;
}

.status-completed .timeline-card {
  border-color: rgba(22, 163, 74, 0.18);
  background: linear-gradient(180deg, #ffffff 0%, #f0fdf4 100%);
}

.status-warning .timeline-card {
  border-color: rgba(220, 38, 38, 0.18);
  background: linear-gradient(180deg, #ffffff 0%, #fef2f2 100%);
}

.status-waiting_feedback .timeline-card,
.status-running .timeline-card {
  border-color: rgba(217, 119, 6, 0.16);
  background: linear-gradient(180deg, #ffffff 0%, #fffbeb 100%);
}

:deep(.clickable-row) {
  cursor: pointer;
}
</style>
