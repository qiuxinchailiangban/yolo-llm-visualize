<template>
  <div class="page">
    <div class="action-bar">
      <el-button type="primary" plain :loading="checking" @click="onSelfCheck">环境自检</el-button>
      <el-button type="warning" :loading="batchSending" @click="openBatchReminderDialog">
        一键提醒今日未填（{{ dashboard?.remindableCount ?? 0 }}）
      </el-button>
      <el-button type="success" @click="openManualRuleDialog">手动任务检测与发送</el-button>
      <span v-if="lastCheckedAt" class="hint">上次自检：{{ lastCheckedAt }}</span>
    </div>

    <el-card v-if="checkResult" class="self-check-card">
      <template #header>
        <div class="self-check-header">
          <el-tag :type="checkResult.overallOk ? 'success' : 'danger'" effect="dark">
            {{ checkResult.overallOk ? "整体通过" : "存在阻塞项" }}
          </el-tag>
          <span class="self-check-tip">
            点开每一行查看详情；带"立即修复"提示的项必须先解决，别人扫码才能用。
          </span>
        </div>
      </template>
      <el-table :data="checkResult.items" size="small" :row-class-name="rowClass">
        <el-table-column prop="name" label="检查项" width="240" />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" />
        <el-table-column prop="hint" label="立即修复" />
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card><div class="metric"><span>今日手术患者</span><strong>{{ dashboard?.surgeryTodayCount ?? 0 }}</strong></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="metric"><span>今日待填问卷</span><strong>{{ dashboard?.questionnaireDueTodayCount ?? 0 }}</strong></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="metric"><span>可发送提醒</span><strong>{{ dashboard?.remindableCount ?? 0 }}</strong></div></el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="batchDialogVisible" title="一键提醒今日未填" width="720px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="发送前必读"
        description="该按钮会按【患者姓名】在微信里搜索会话逐个发文本提醒。需要 desktop worker 正在运行，且倒计时结束前把微信窗口切到前台。2 小时内已经提醒过的任务会自动跳过。"
      />

      <el-form label-width="140px" class="batch-form">
        <el-form-item label="发送前倒计时">
          <el-input-number v-model="batchForm.countdownSeconds" :min="0" :max="300" />
          <span class="hint">秒；点击确认后，请在倒计时结束前把微信切到前台</span>
        </el-form-item>
        <el-form-item label="跳过近期已提醒">
          <el-switch v-model="batchForm.skipRecentlyReminded" />
          <el-input-number
            v-if="batchForm.skipRecentlyReminded"
            v-model="batchForm.recentWindowHours"
            :min="1"
            :max="72"
            style="margin-left: 12px"
          />
          <span v-if="batchForm.skipRecentlyReminded" class="hint">小时内已在队列 / 已发送的任务会跳过</span>
        </el-form-item>
        <el-form-item label="提醒文案模板">
          <el-input
            v-model="batchForm.contentTemplate"
            type="textarea"
            :rows="4"
            placeholder="留空则使用默认模板。可用占位符：{patientName} {stageName} {dueDate}"
          />
        </el-form-item>
      </el-form>

      <div v-if="batchResult" class="batch-result">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="总数">{{ batchResult.total }}</el-descriptions-item>
          <el-descriptions-item label="入队">
            <el-tag type="success" size="small">{{ batchResult.queued }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="跳过">
            <el-tag type="info" size="small">{{ batchResult.skipped }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-tag type="danger" size="small">{{ batchResult.failed }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-table :data="batchResult.items" size="small" class="mt12" max-height="360">
          <el-table-column prop="patientName" label="姓名" width="100" />
          <el-table-column prop="stageName" label="阶段" width="120" />
          <el-table-column prop="dueDate" label="应填日" width="110" />
          <el-table-column prop="targetConversation" label="目标会话" width="120" />
          <el-table-column label="结果" width="120">
            <template #default="{ row }">
              <el-tag :type="batchItemTagType(row.status)" size="small">
                {{ batchItemLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="180" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="batchDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="batchSending" @click="submitBatchReminder">
          {{ batchResult ? "再次发送" : "确认发送" }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="manualRuleDialogVisible" title="手动任务检测与发送" width="980px" destroy-on-close>
      <el-form label-width="120px">
        <el-form-item label="手动任务">
          <el-select v-model="manualRuleForm.ruleIds" multiple collapse-tags style="width: 100%" placeholder="选择要检测的手动任务">
            <el-option v-for="rule in manualRules" :key="rule.id" :label="rule.ruleName" :value="rule.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="患者范围">
          <el-select v-model="manualRuleForm.patientIds" multiple collapse-tags style="width: 100%" placeholder="选择患者；留空则检测全部患者">
            <el-option
              v-for="patient in dashboard?.allPatients ?? []"
              :key="patient.patientId"
              :label="`${patient.patientName} / ${patient.patientId}`"
              :value="patient.patientId"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="manual-rule-actions">
        <el-button type="primary" :loading="manualRuleDetecting" @click="detectManualRules">先检测候选任务</el-button>
        <el-button
          type="success"
          :loading="manualRuleExecuting"
          :disabled="!selectedManualCandidates.length"
          @click="executeManualRules"
        >
          一键发选中项（{{ selectedManualCandidates.length }}）
        </el-button>
      </div>

      <el-table
        v-if="manualCandidates.length"
        :data="manualCandidates"
        class="mt12"
        max-height="420"
        @selection-change="onManualSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="ruleName" label="任务" width="180" />
        <el-table-column prop="patientName" label="患者" width="140" />
        <el-table-column prop="triggerType" label="触发方式" width="150" />
        <el-table-column prop="targetConversation" label="目标会话" width="160" />
        <el-table-column prop="contentPreview" label="发送内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sourceMessagePreview" label="命中消息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="detectedReason" label="检测原因" min-width="160" />
      </el-table>

      <el-empty v-else description="先选择任务和患者，再点“先检测候选任务”" />

      <div v-if="manualExecuteResult" class="batch-result">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="总数">{{ manualExecuteResult.total }}</el-descriptions-item>
          <el-descriptions-item label="入队">{{ manualExecuteResult.queued }}</el-descriptions-item>
          <el-descriptions-item label="跳过">{{ manualExecuteResult.skipped }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="manualRuleDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-row :gutter="16" class="mt16">
      <el-col :span="8">
        <el-card header="今天要手术的患者">
          <el-table :data="dashboard?.surgeriesToday ?? []" size="small">
            <el-table-column prop="patientId" label="患者ID" />
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="dueDate" label="手术日" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="今天应填未填">
          <el-table :data="dashboard?.questionnaireDueToday ?? []" size="small">
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="stageName" label="阶段" />
            <el-table-column prop="dueDate" label="应填日" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="可发送提醒">
          <el-table :data="dashboard?.remindablePatients ?? []" size="small">
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="stageName" label="阶段" />
            <el-table-column prop="remark" label="状态" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { fetchDashboard } from "../api/dashboard";
import { runSelfCheck, type SelfCheckItem, type SelfCheckResult } from "../api/selfCheck";
import {
  detectManualMessageTriggerCandidates,
  executeManualMessageTriggerCandidates,
  fetchManualMessageTriggerRules,
} from "../api/messageTriggerRule";
import {
  sendDailyBatchReminder,
  type DailyBatchReminderResult,
} from "../api/reminder";
import type {
  DashboardData,
  MessageTriggerManualCandidate,
  MessageTriggerManualExecuteResult,
  MessageTriggerRule,
} from "../types";

const dashboard = ref<DashboardData>();
const checkResult = ref<SelfCheckResult>();
const checking = ref(false);
const lastCheckedAt = ref<string>();

const batchDialogVisible = ref(false);
const batchSending = ref(false);
const batchResult = ref<DailyBatchReminderResult>();
const manualRuleDialogVisible = ref(false);
const manualRules = ref<MessageTriggerRule[]>([]);
const manualCandidates = ref<MessageTriggerManualCandidate[]>([]);
const selectedManualCandidates = ref<MessageTriggerManualCandidate[]>([]);
const manualRuleDetecting = ref(false);
const manualRuleExecuting = ref(false);
const manualExecuteResult = ref<MessageTriggerManualExecuteResult>();
const batchForm = reactive({
  countdownSeconds: 10,
  skipRecentlyReminded: true,
  recentWindowHours: 2,
  contentTemplate: "",
});
const manualRuleForm = reactive({
  ruleIds: [] as number[],
  patientIds: [] as string[],
});

async function loadData() {
  dashboard.value = await fetchDashboard();
}

async function loadManualRules() {
  manualRules.value = await fetchManualMessageTriggerRules();
}

async function onSelfCheck() {
  checking.value = true;
  try {
    checkResult.value = await runSelfCheck();
    lastCheckedAt.value = new Date().toLocaleString();
    if (checkResult.value.overallOk) {
      ElMessage.success("自检通过：当前环境可以让别人扫码填问卷");
    } else {
      ElMessage.warning("自检发现阻塞项，请按【立即修复】逐项处理");
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "自检失败");
  } finally {
    checking.value = false;
  }
}

function levelLabel(level: SelfCheckItem["level"]) {
  if (level === "OK") return "OK";
  if (level === "WARN") return "提醒";
  return "阻塞";
}

function tagType(level: SelfCheckItem["level"]) {
  if (level === "OK") return "success";
  if (level === "WARN") return "warning";
  return "danger";
}

function rowClass({ row }: { row: SelfCheckItem }) {
  if (row.level === "ERROR") return "row-error";
  if (row.level === "WARN") return "row-warn";
  return "";
}

function openBatchReminderDialog() {
  const count = dashboard.value?.remindableCount ?? 0;
  if (count === 0) {
    ElMessage.info("当前没有需要提醒的任务");
    return;
  }
  batchResult.value = undefined;
  batchDialogVisible.value = true;
}

async function openManualRuleDialog() {
  manualCandidates.value = [];
  selectedManualCandidates.value = [];
  manualExecuteResult.value = undefined;
  await loadManualRules();
  manualRuleDialogVisible.value = true;
}

async function detectManualRules() {
  manualRuleDetecting.value = true;
  try {
    manualCandidates.value = await detectManualMessageTriggerCandidates({
      ruleIds: manualRuleForm.ruleIds,
      patientIds: manualRuleForm.patientIds,
    });
    selectedManualCandidates.value = [];
    manualExecuteResult.value = undefined;
    if (!manualCandidates.value.length) {
      ElMessage.info("当前没有检测到可执行的手动任务");
    } else {
      ElMessage.success(`检测到 ${manualCandidates.value.length} 条可执行候选`);
    }
  } finally {
    manualRuleDetecting.value = false;
  }
}

function onManualSelectionChange(rows: MessageTriggerManualCandidate[]) {
  selectedManualCandidates.value = rows;
}

async function executeManualRules() {
  if (!selectedManualCandidates.value.length) {
    ElMessage.warning("请先选择要发送的候选任务");
    return;
  }
  manualRuleExecuting.value = true;
  try {
    manualExecuteResult.value = await executeManualMessageTriggerCandidates(
      selectedManualCandidates.value.map((item) => ({
        ruleId: item.ruleId,
        patientId: item.patientId,
        candidateKey: item.candidateKey,
        sourceMessageKey: item.sourceMessageKey,
      })),
    );
    ElMessage.success(`已入队 ${manualExecuteResult.value.queued} 条手动任务`);
    await loadData();
  } finally {
    manualRuleExecuting.value = false;
  }
}

async function submitBatchReminder() {
  try {
    await ElMessageBox.confirm(
      `即将向今日/逾期未填的约 ${dashboard.value?.remindableCount ?? 0} 个任务` +
        `按【患者姓名】在微信里搜索会话并发送提醒。确认继续吗？`,
      "确认发送",
      { type: "warning", confirmButtonText: "确定发送", cancelButtonText: "再看看" }
    );
  } catch {
    return;
  }

  batchSending.value = true;
  try {
    batchResult.value = await sendDailyBatchReminder({
      countdownSeconds: batchForm.countdownSeconds,
      skipRecentlyReminded: batchForm.skipRecentlyReminded,
      recentWindowHours: batchForm.recentWindowHours,
      contentTemplate: batchForm.contentTemplate.trim() || undefined,
    });
    const r = batchResult.value;
    if (r.queued > 0) {
      ElMessage.success(
        `已入队 ${r.queued} 条，请立即切到微信前台，worker 会按倒计时依次发送`
      );
    } else {
      ElMessage.warning(
        `没有新提醒入队（跳过 ${r.skipped}，失败 ${r.failed}）`
      );
    }
    await loadData();
  } catch (error: any) {
    ElMessage.error(
      error?.response?.data?.message || error?.message || "批量提醒失败"
    );
  } finally {
    batchSending.value = false;
  }
}

function batchItemLabel(status: string) {
  switch (status) {
    case "QUEUED":
      return "已入队";
    case "SKIPPED_RECENT_REMINDER":
      return "近期已提醒";
    case "SKIPPED_NO_CONTACT":
      return "跳过";
    case "FAILED":
      return "失败";
    default:
      return status;
  }
}

function batchItemTagType(status: string) {
  switch (status) {
    case "QUEUED":
      return "success";
    case "SKIPPED_RECENT_REMINDER":
    case "SKIPPED_NO_CONTACT":
      return "info";
    case "FAILED":
      return "danger";
    default:
      return "info";
  }
}

onMounted(async () => {
  await loadData();
  await loadManualRules();
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-bar .hint {
  color: #909399;
  font-size: 12px;
}

.self-check-card :deep(.el-card__header) {
  padding: 12px 16px;
}

.self-check-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.self-check-tip {
  color: #606266;
  font-size: 12px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}

.metric strong {
  font-size: 28px;
  color: #409eff;
}

.mt16 {
  margin-top: 16px;
}

.mt12 {
  margin-top: 12px;
}

.batch-form {
  margin-top: 16px;
}

.batch-form .hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.batch-result {
  margin-top: 16px;
}

.manual-rule-actions {
  display: flex;
  gap: 12px;
}

:deep(.row-error) {
  background-color: #fef0f0 !important;
}

:deep(.row-warn) {
  background-color: #fdf6ec !important;
}
</style>
