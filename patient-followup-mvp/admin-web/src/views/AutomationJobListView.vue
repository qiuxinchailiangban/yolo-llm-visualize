<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span>最近 100 条自动化任务</span>
          <el-button type="primary" @click="loadJobs">刷新</el-button>
        </div>
      </template>

      <el-table :data="jobs">
        <el-table-column prop="jobNo" label="任务号" width="220" />
        <el-table-column prop="jobType" label="类型" width="150" />
        <el-table-column prop="channel" label="通道" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="workerId" label="执行器" width="180" />
        <el-table-column prop="plannedAt" label="计划时间" width="180" />
        <el-table-column prop="startedAt" label="开始时间" width="180" />
        <el-table-column prop="finishedAt" label="结束时间" width="180" />
        <el-table-column prop="lastError" label="错误信息" min-width="220" />
        <el-table-column label="日志" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLogDialog(row)">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="自动化任务日志" width="760px" destroy-on-close>
      <el-descriptions v-if="currentJob" :column="1" border>
        <el-descriptions-item label="任务号">{{ currentJob.jobNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentJob.status }}</el-descriptions-item>
        <el-descriptions-item label="执行器">{{ currentJob.workerId || "-" }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentJob.lastError">{{ currentJob.lastError }}</el-descriptions-item>
        <el-descriptions-item label="日志" v-if="currentJob.executionLog">
          <pre class="execution-log">{{ currentJob.executionLog }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { fetchAutomationJobs } from "../api/automation";
import type { AutomationJob } from "../types";

const jobs = ref<AutomationJob[]>([]);
const currentJob = ref<AutomationJob>();
const logDialogVisible = ref(false);

const statusType: Record<string, string> = {
  QUEUED: "info",
  RUNNING: "warning",
  SUCCESS: "success",
  FAILED: "danger",
  CANCELLED: "info",
};

async function loadJobs() {
  jobs.value = await fetchAutomationJobs();
}

function openLogDialog(job: AutomationJob) {
  currentJob.value = job;
  logDialogVisible.value = true;
}

onMounted(loadJobs);
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

.execution-log {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, "Courier New", monospace;
}
</style>
