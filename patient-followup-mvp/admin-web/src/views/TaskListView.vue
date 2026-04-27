<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="filters">
            <el-input v-model="filters.keyword" placeholder="患者姓名 / patient_id / 任务号" clearable />
            <el-select v-model="filters.status" placeholder="状态" clearable>
              <el-option label="待填写" value="PENDING" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已逾期" value="OVERDUE" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-select v-model="filters.stageId" placeholder="阶段" clearable>
              <el-option v-for="stage in stages" :key="stage.id" :label="stage.stageName" :value="stage.id" />
            </el-select>
            <el-date-picker v-model="filters.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="应填日期" />
            <el-button type="primary" @click="loadTasks">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tasks">
        <el-table-column prop="taskNo" label="任务号" width="200" />
        <el-table-column prop="patientId" label="Patient ID" width="160" />
        <el-table-column prop="patientName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="stageName" label="阶段" width="120" />
        <el-table-column prop="templateName" label="模板" width="160" />
        <el-table-column prop="dueDate" label="应填日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finishedAt" label="完成时间" min-width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReminderDialog(row)">发送提醒</el-button>
            <el-button link type="info" @click="openReminderLogsDialog(row)">发送日志</el-button>
            <el-button link type="success" @click="changeStatus(row.taskNo, 'COMPLETED')">标记完成</el-button>
            <el-button link type="warning" @click="changeStatus(row.taskNo, 'PENDING')">设为待填</el-button>
            <el-button link type="danger" @click="changeStatus(row.taskNo, 'CANCELLED')">取消任务</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="reminderDialogVisible" title="发送微信提醒" width="640px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="患者">
          <el-input :model-value="currentReminderTask ? `${currentReminderTask.patientName} / ${currentReminderTask.patientId}` : ''" disabled />
        </el-form-item>
        <el-form-item label="任务">
          <el-input :model-value="currentReminderTask ? `${currentReminderTask.stageName} / ${currentReminderTask.dueDate}` : ''" disabled />
        </el-form-item>
        <el-form-item label="目标会话" required>
          <el-input v-model="reminderForm.targetConversation" placeholder="优先使用患者已绑定群，未绑定时可手动填写" />
        </el-form-item>
        <el-form-item label="提醒内容" required>
          <el-input
            v-model="reminderForm.content"
            type="textarea"
            :rows="6"
            resize="none"
            placeholder="请输入要发送的微信提醒文案"
          />
        </el-form-item>
        <el-form-item label="发送前倒计时">
          <el-input-number v-model="reminderForm.countdownSeconds" :min="0" :max="120" />
          <span class="countdown-tip">点击发送后，请在倒计时结束前切回微信窗口</span>
        </el-form-item>
      </el-form>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="当前点击后只会创建自动化任务；需要本地 desktop worker 正在运行，且倒计时结束前把微信窗口切到前台。"
      />

      <div v-if="reminderResult" class="reminder-result">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="发送状态">{{ reminderResult.status }}</el-descriptions-item>
          <el-descriptions-item label="目标会话">{{ reminderResult.targetConversation }}</el-descriptions-item>
          <el-descriptions-item label="返回信息">{{ reminderResult.message }}</el-descriptions-item>
            <el-descriptions-item v-if="reminderResult.countdownSeconds !== undefined" label="倒计时">
              {{ reminderResult.countdownSeconds }} 秒
            </el-descriptions-item>
            <el-descriptions-item v-if="reminderResult.startedAt" label="开始时间">{{ reminderResult.startedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="reminderResult.sentAt" label="发送时间">{{ reminderResult.sentAt }}</el-descriptions-item>
            <el-descriptions-item v-if="reminderResult.finishedAt" label="结束时间">{{ reminderResult.finishedAt }}</el-descriptions-item>
            <el-descriptions-item v-if="reminderResult.commandLine" label="执行命令">{{ reminderResult.commandLine }}</el-descriptions-item>
          <el-descriptions-item v-if="reminderResult.output" label="脚本输出">{{ reminderResult.output }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="reminderDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="reminderSending" @click="submitReminder">确认发送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reminderLogsVisible" title="提醒发送日志" width="880px" destroy-on-close>
      <el-empty v-if="!reminderLogs.length" description="当前任务还没有提醒日志" />
      <div v-else class="reminder-logs">
        <el-table :data="reminderLogs">
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="targetConversation" label="目标会话" width="160" />
          <el-table-column prop="contentPreview" label="内容摘要" min-width="180" />
          <el-table-column prop="plannedAt" label="计划时间" width="180" />
          <el-table-column prop="sentAt" label="发送时间" width="180" />
        </el-table>
        <el-collapse class="logs-collapse">
          <el-collapse-item
            v-for="log in reminderLogs"
            :key="log.id"
            :title="`${log.status} / ${log.targetConversation || '-'} / ${log.plannedAt}`"
            :name="String(log.id)"
          >
            <el-descriptions :column="1" border>
              <el-descriptions-item label="失败原因" v-if="log.failReason">{{ log.failReason }}</el-descriptions-item>
              <el-descriptions-item label="执行命令" v-if="log.commandLine">{{ log.commandLine }}</el-descriptions-item>
              <el-descriptions-item label="执行日志" v-if="log.executionLog">
                <pre class="execution-log">{{ log.executionLog }}</pre>
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button @click="reminderLogsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { onMounted, reactive, ref } from "vue";
import { fetchStages } from "../api/stage";
import { fetchTaskReminders, fetchTasks, sendTaskReminder, updateTaskStatus } from "../api/task";
import type { ReminderTaskLog, Stage, TaskItem, TaskReminderSendResult } from "../types";

const stages = ref<Stage[]>([]);
const tasks = ref<TaskItem[]>([]);
const reminderDialogVisible = ref(false);
const reminderLogsVisible = ref(false);
const reminderSending = ref(false);
const currentReminderTask = ref<TaskItem>();
const reminderResult = ref<TaskReminderSendResult>();
const reminderLogs = ref<ReminderTaskLog[]>([]);
const filters = reactive({
  keyword: "",
  status: "",
  stageId: undefined as number | undefined,
  dueDate: "",
});
const reminderForm = reactive({
  targetConversation: "",
  content: "",
  countdownSeconds: 5,
});

const statusType: Record<string, string> = {
  PENDING: "info",
  IN_PROGRESS: "warning",
  COMPLETED: "success",
  OVERDUE: "danger",
  CANCELLED: "info",
};

async function loadTasks() {
  tasks.value = await fetchTasks({
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    stageId: filters.stageId,
    dueDate: filters.dueDate || undefined,
  });
}

async function loadStages() {
  stages.value = await fetchStages();
}

async function changeStatus(taskNo: string, status: string) {
  await updateTaskStatus(taskNo, status);
  ElMessage.success("任务状态已更新");
  await loadTasks();
}

function buildDefaultReminderContent(task: TaskItem) {
  return [
    `${task.patientName}您好，`,
    `您有一份${task.stageName}随访问卷待填写。`,
    `应填写日期：${task.dueDate}。`,
    "请您尽快完成填写，如已完成可忽略本消息。",
  ].join("");
}

function openReminderDialog(task: TaskItem) {
  currentReminderTask.value = task;
  reminderResult.value = undefined;
  reminderForm.targetConversation = task.preferredConversation || task.patientName || "";
  reminderForm.content = buildDefaultReminderContent(task);
  reminderForm.countdownSeconds = 5;
  reminderDialogVisible.value = true;
}

async function openReminderLogsDialog(task: TaskItem) {
  reminderLogs.value = await fetchTaskReminders(task.taskNo);
  reminderLogsVisible.value = true;
}

async function submitReminder() {
  if (!currentReminderTask.value) {
    return;
  }
  if (!reminderForm.targetConversation.trim()) {
    ElMessage.warning("请填写目标会话");
    return;
  }
  if (!reminderForm.content.trim()) {
    ElMessage.warning("请填写提醒内容");
    return;
  }

  reminderSending.value = true;
  try {
    const result = await sendTaskReminder(currentReminderTask.value.taskNo, {
      targetConversation: reminderForm.targetConversation.trim(),
      content: reminderForm.content.trim(),
      countdownSeconds: reminderForm.countdownSeconds ?? 0,
    });
    reminderResult.value = result;
    reminderLogs.value = await fetchTaskReminders(currentReminderTask.value.taskNo);
    if (result.status === "QUEUED") {
      ElMessage.success(`提醒任务已入队，请在 ${result.countdownSeconds ?? 0} 秒内切回微信窗口`);
    } else if (result.status === "SENT") {
      ElMessage.success("提醒已发送");
    } else {
      ElMessage.error(result.message || "提醒发送失败");
    }
  } finally {
    reminderSending.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadStages(), loadTasks()]);
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}

.reminder-result {
  margin-top: 16px;
}

.countdown-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.reminder-logs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.logs-collapse {
  margin-top: 8px;
}

.execution-log {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, "Courier New", monospace;
}
</style>
