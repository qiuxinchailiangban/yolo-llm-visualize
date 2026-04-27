<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" placeholder="按姓名或patient_id搜索" clearable @keyup.enter="loadPatients" />
            <el-button type="primary" @click="loadPatients">搜索</el-button>
          </div>
          <div class="toolbar-actions">
            <el-button @click="openImport">CSV 导入</el-button>
            <el-button type="success" @click="openCreate">新增患者</el-button>
          </div>
        </div>
      </template>

      <el-table :data="patients" @row-click="showDetail">
        <el-table-column prop="patientId" label="Patient ID" width="170" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="90" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="surgeryDate" label="手术日期" width="120" />
        <el-table-column prop="surgeryScheduleTag" label="手术场次" width="120" />
        <el-table-column prop="surgeryTimeText" label="手术时间" width="120" />
        <el-table-column prop="wechatGroupName" label="绑定群" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="sourceChannel" label="来源" width="140" />
        <el-table-column prop="diagnosis" label="诊断" min-width="200" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="success" @click.stop="openProcess(row)">流程</el-button>
              <el-button link type="primary" @click.stop="showDetail(row)">详情</el-button>
              <el-button link type="warning" @click.stop="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click.stop="confirmDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="患者详情" size="50%">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Patient ID">{{ detail.patient.patientId }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.patient.name }}</el-descriptions-item>
          <el-descriptions-item label="手术日期">{{ detail.patient.surgeryDate }}</el-descriptions-item>
          <el-descriptions-item label="手术场次">{{ detail.patient.surgeryScheduleTag || "-" }}</el-descriptions-item>
          <el-descriptions-item label="手术时间">{{ detail.patient.surgeryTimeText || "-" }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.patient.status }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.patient.phone }}</el-descriptions-item>
          <el-descriptions-item label="诊断">{{ detail.patient.diagnosis }}</el-descriptions-item>
          <el-descriptions-item label="绑定群名称">{{ detail.patient.wechatGroupName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="群唯一标识">{{ detail.patient.wechatChatroomUsername || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>问卷任务</el-divider>
        <el-table :data="detail.tasks">
          <el-table-column prop="taskNo" label="任务编号" width="200" />
          <el-table-column prop="stageName" label="阶段" width="120" />
          <el-table-column prop="templateName" label="模板" width="160" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="dueDate" label="应填日期" width="120" />
          <el-table-column prop="finishedAt" label="完成时间" min-width="180" />
        </el-table>

        <el-divider>最近群聊消息</el-divider>
        <el-empty v-if="!detail.recentChatMessages.length" description="当前还没有归属到该患者的群聊消息" />
        <el-table v-else :data="detail.recentChatMessages" max-height="320">
          <el-table-column prop="messageTime" label="消息时间" width="180" />
          <el-table-column prop="senderDisplayName" label="发送者" width="140" />
          <el-table-column prop="direction" label="方向" width="100" />
          <el-table-column prop="contentPreview" label="内容" min-width="260" show-overflow-tooltip />
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" :title="formDialogTitle" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别"><el-input v-model="form.gender" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="手术日期"><el-date-picker v-model="form.surgeryDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="手术场次"><el-input v-model="form.surgeryScheduleTag" placeholder="如：第1台 / 第2台" /></el-form-item>
        <el-form-item label="手术时间"><el-input v-model="form.surgeryTimeText" placeholder="如：07:30 / 上午首台" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="form.diagnosis" type="textarea" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="form.sourceChannel" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPatientForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入患者/手术名单 CSV" width="920px">
      <div class="import-panel">
        <el-alert type="info" :closable="false" show-icon>
          <div>支持两种方式：直接粘贴 CSV 文本，或选择本地 `.csv` 文件。</div>
          <div>列顺序按模板：姓名,手机号,手术日期,性别,出生日期,诊断,来源,手术场次,手术时间</div>
          <div>日期格式统一为 `YYYY-MM-DD`，导入后会自动创建或更新患者，并生成随访任务。</div>
        </el-alert>

        <div class="import-actions">
          <el-button @click="fillExample">填入示例</el-button>
          <el-button @click="triggerFileSelect">选择 CSV 文件</el-button>
          <el-button @click="csvText = ''">清空内容</el-button>
          <input ref="fileInputRef" class="hidden-input" type="file" accept=".csv,text/csv" @change="handleFileSelect" />
        </div>

        <el-input
          v-model="csvText"
          type="textarea"
          :rows="10"
          resize="none"
          placeholder="请粘贴 CSV 内容，首行需要表头。"
        />

        <div v-if="importResult" class="import-summary">
          <el-row :gutter="12">
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>总行数</span><strong>{{ importResult.totalRows }}</strong></div></el-card></el-col>
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>成功</span><strong>{{ importResult.successRows }}</strong></div></el-card></el-col>
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>新增</span><strong>{{ importResult.createdCount }}</strong></div></el-card></el-col>
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>更新</span><strong>{{ importResult.updatedCount }}</strong></div></el-card></el-col>
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>跳过</span><strong>{{ importResult.skippedCount }}</strong></div></el-card></el-col>
            <el-col :span="4"><el-card shadow="never"><div class="summary-item"><span>生成任务</span><strong>{{ importResult.totalTasksGenerated }}</strong></div></el-card></el-col>
          </el-row>

          <el-table :data="importResult.rows" max-height="320">
            <el-table-column prop="rowNumber" label="行号" width="70" />
            <el-table-column prop="patientName" label="姓名" width="120" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column prop="surgeryDate" label="手术日期" width="120" />
            <el-table-column label="结果" width="110">
              <template #default="{ row }">
                <el-tag :type="actionTagType(row.action)">{{ actionLabel(row.action) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="patientId" label="Patient ID" width="170" />
            <el-table-column prop="taskCount" label="任务数" width="90" />
            <el-table-column prop="message" label="说明" min-width="220" />
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createPatient, deletePatient, fetchPatientDetail, fetchPatients, importPatientCsv, updatePatient } from "../api/patient";
import type { Patient, PatientDetail, PatientForm, PatientImportResult } from "../types";

const router = useRouter();
const keyword = ref("");
const patients = ref<Patient[]>([]);
const detail = ref<PatientDetail>();
const detailVisible = ref(false);
const createVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const editingPatientId = ref<string>("");
const importVisible = ref(false);
const importLoading = ref(false);
const csvText = ref("");
const importResult = ref<PatientImportResult>();
const fileInputRef = ref<HTMLInputElement | null>(null);
const form = reactive<PatientForm>({
  name: "",
  gender: "",
  phone: "",
  birthDate: "",
  surgeryDate: "",
  surgeryScheduleTag: "",
  surgeryTimeText: "",
  diagnosis: "",
  sourceChannel: "ADMIN",
});
const formDialogTitle = computed(() => (dialogMode.value === "create" ? "新增患者" : "编辑患者"));

async function loadPatients() {
  patients.value = await fetchPatients(keyword.value);
}

async function showDetail(row: Patient) {
  detail.value = await fetchPatientDetail(row.patientId);
  detailVisible.value = true;
}

function openProcess(row: Patient) {
  router.push("/patient-processes");
  window.setTimeout(() => {
    window.dispatchEvent(new CustomEvent("patient-process:open", { detail: { patientId: row.patientId } }));
  }, 80);
}

function openCreate() {
  dialogMode.value = "create";
  editingPatientId.value = "";
  resetForm();
  createVisible.value = true;
}

function openEdit(row: Patient) {
  dialogMode.value = "edit";
  editingPatientId.value = row.patientId;
  Object.assign(form, {
    name: row.name ?? "",
    gender: row.gender ?? "",
    phone: row.phone ?? "",
    birthDate: row.birthDate ?? "",
    surgeryDate: row.surgeryDate ?? "",
    surgeryScheduleTag: row.surgeryScheduleTag ?? "",
    surgeryTimeText: row.surgeryTimeText ?? "",
    diagnosis: row.diagnosis ?? "",
    sourceChannel: row.sourceChannel ?? "ADMIN",
  });
  createVisible.value = true;
}

function openImport() {
  importVisible.value = true;
}

function fillExample() {
  csvText.value = [
    "姓名,手机号,手术日期,性别,出生日期,诊断,来源,手术场次,手术时间",
    "张三,13800000001,2026-04-25,男,1980-01-10,膝关节置换,CSV_IMPORT,第1台,07:30",
    "李四,13800000002,2026-04-28,女,1976-08-21,白内障,CSV_IMPORT,第3台,10:20",
  ].join("\n");
}

function triggerFileSelect() {
  fileInputRef.value?.click();
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    const result = reader.result;
    if (typeof result === "string") {
      csvText.value = result.replace(/^\uFEFF/, "");
      ElMessage.success(`已读取文件：${file.name}`);
    } else {
      ElMessage.error("CSV 文件读取失败");
    }
  };
  reader.onerror = () => ElMessage.error("CSV 文件读取失败");
  reader.readAsText(file, "utf-8");
  input.value = "";
}

function actionLabel(action: string) {
  if (action === "CREATED") {
    return "已新增";
  }
  if (action === "UPDATED") {
    return "已更新";
  }
  return "已跳过";
}

function actionTagType(action: string): "success" | "warning" | "info" {
  if (action === "CREATED") {
    return "success";
  }
  if (action === "UPDATED") {
    return "warning";
  }
  return "info";
}

function resetForm() {
  Object.assign(form, {
    name: "",
    gender: "",
    phone: "",
    birthDate: "",
    surgeryDate: "",
    surgeryScheduleTag: "",
    surgeryTimeText: "",
    diagnosis: "",
    sourceChannel: "ADMIN",
  });
}

async function submitPatientForm() {
  if (dialogMode.value === "create") {
    await createPatient(form);
    ElMessage.success("患者创建成功");
  } else {
    await updatePatient(editingPatientId.value, form);
    ElMessage.success("患者信息更新成功");
    if (detailVisible.value && detail.value?.patient.patientId === editingPatientId.value) {
      detail.value = await fetchPatientDetail(editingPatientId.value);
    }
  }
  createVisible.value = false;
  await loadPatients();
}

async function confirmDelete(row: Patient) {
  try {
    await ElMessageBox.confirm(
      `确认删除患者“${row.name}”吗？该患者下的任务、提醒和问卷记录也会一起删除。`,
      "删除患者",
      {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  } catch {
    return;
  }

  try {
    await deletePatient(row.patientId);
    ElMessage.success("患者删除成功");
    if (detailVisible.value && detail.value?.patient.patientId === row.patientId) {
      detailVisible.value = false;
      detail.value = undefined;
    }
    await loadPatients();
  } catch (error) {
    const message = extractDeleteErrorMessage(error);
    ElMessage.error(message);
    console.error("[patient-delete] failed", error);
  }
}

function extractDeleteErrorMessage(error: unknown): string {
  if (error && typeof error === "object") {
    const anyErr = error as {
      response?: { data?: { message?: string } };
      message?: string;
    };
    const backendMsg = anyErr.response?.data?.message;
    if (backendMsg) {
      return `删除失败：${backendMsg}`;
    }
    if (anyErr.message) {
      return `删除失败：${anyErr.message}`;
    }
  }
  return "删除失败，请查看浏览器控制台或后端日志";
}

async function submitImport() {
  if (!csvText.value.trim()) {
    ElMessage.warning("请先粘贴或选择 CSV 内容");
    return;
  }

  importLoading.value = true;
  try {
    importResult.value = await importPatientCsv(csvText.value);
    ElMessage.success("CSV 导入完成");
    await loadPatients();
  } finally {
    importLoading.value = false;
  }
}

onMounted(loadPatients);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  width: 420px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.import-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-actions {
  display: flex;
  gap: 12px;
}

.hidden-input {
  display: none;
}

.import-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: center;
}

.summary-item strong {
  font-size: 22px;
  color: #409eff;
}
</style>
