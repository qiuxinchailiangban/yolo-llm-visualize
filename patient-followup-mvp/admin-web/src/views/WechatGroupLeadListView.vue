<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div>
            <div class="title">微信群线索</div>
            <div class="subtitle">查看新发现的患者微信群，并手动转患者或绑定已有患者。</div>
          </div>
          <el-button type="primary" @click="loadLeads">刷新</el-button>
        </div>
      </template>

      <el-table :data="leads" v-loading="loading">
        <el-table-column prop="rawGroupName" label="群名称" min-width="240" />
        <el-table-column prop="groupStage" label="阶段" width="110">
          <template #default="{ row }">
            <el-tag :type="stageTagType(row.groupStage)">{{ stageLabel(row.groupStage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.parseStatus === 'PARSED' ? 'success' : 'warning'">
              {{ row.parseStatus === "PARSED" ? "已解析" : "待处理" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assistantDoctorName" label="二线医生" width="100" />
        <el-table-column prop="patientName" label="患者姓名" width="100" />
        <el-table-column prop="surgerySite" label="手术部位" min-width="150" />
        <el-table-column prop="eventDateText" label="日期文本" width="100" />
        <el-table-column label="已绑定患者" min-width="180">
          <template #default="{ row }">
            <span v-if="row.linkedPatientId">{{ row.linkedPatientName || "-" }} / {{ row.linkedPatientId }}</span>
            <span v-else class="muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastMessageSnippet" label="最近消息片段" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="最近发现" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                v-if="canCreatePatient(row)"
                link
                type="success"
                @click="handleCreatePatient(row)"
              >
                转患者
              </el-button>
              <el-button
                link
                type="primary"
                @click="openBindDialog(row)"
              >
                {{ row.linkedPatientId ? "更换绑定" : "绑定患者" }}
              </el-button>
              <el-button
                v-if="row.linkedPatientId"
                link
                type="warning"
                @click="handleUnbindPatient(row)"
              >
                解绑
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="bindDialogVisible" title="绑定已有患者" width="720px">
      <div class="bind-panel">
        <el-alert
          v-if="selectedLead"
          type="info"
          :closable="false"
          show-icon
        >
          当前线索：{{ selectedLead.rawGroupName || selectedLead.chatroomDisplayName || selectedLead.chatroomUsername }}
        </el-alert>

        <div class="bind-search">
          <el-input
            v-model="patientKeyword"
            placeholder="按姓名或 patient_id 搜索患者"
            clearable
            @keyup.enter="searchPatients"
          />
          <el-button type="primary" @click="searchPatients">搜索</el-button>
        </div>

        <el-table :data="candidatePatients" max-height="360" @row-dblclick="selectPatient">
          <el-table-column width="60">
            <template #default="{ row }">
              <el-radio :model-value="selectedPatientId" :label="row.patientId" @change="() => selectPatient(row)">
                &nbsp;
              </el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="patientId" label="Patient ID" width="170" />
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="surgeryDate" label="手术日期" width="120" />
          <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedPatientId" @click="handleBindPatient">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { onMounted, ref } from "vue";
import { fetchPatients } from "../api/patient";
import {
  bindPatientToWechatGroupLead,
  createPatientFromWechatGroupLead,
  fetchWechatGroupLeads,
  unbindPatientFromWechatGroupLead,
} from "../api/wechatGroupLead";
import type { Patient, WechatGroupLead } from "../types";

const leads = ref<WechatGroupLead[]>([]);
const loading = ref(false);
const bindDialogVisible = ref(false);
const selectedLead = ref<WechatGroupLead>();
const patientKeyword = ref("");
const candidatePatients = ref<Patient[]>([]);
const selectedPatientId = ref("");

async function loadLeads() {
  loading.value = true;
  try {
    leads.value = await fetchWechatGroupLeads();
  } finally {
    loading.value = false;
  }
}

function stageLabel(stage?: string) {
  if (stage === "PRE_OP") return "未手术";
  if (stage === "POST_OP") return "已手术";
  if (stage === "UNFIT") return "不能手术";
  return "-";
}

function stageTagType(stage?: string): "info" | "success" | "warning" {
  if (stage === "POST_OP") return "success";
  if (stage === "UNFIT") return "warning";
  return "info";
}

function canCreatePatient(row: WechatGroupLead) {
  return !row.linkedPatientId && row.parseStatus === "PARSED" && Boolean(row.patientName);
}

async function handleCreatePatient(row: WechatGroupLead) {
  await ElMessageBox.confirm(
    `确认根据线索“${row.rawGroupName || row.chatroomUsername}”创建患者吗？`,
    "转患者确认",
    { type: "warning" }
  );
  await createPatientFromWechatGroupLead(row.chatroomUsername);
  ElMessage.success("已创建患者并完成绑定");
  await loadLeads();
}

async function openBindDialog(row: WechatGroupLead) {
  selectedLead.value = row;
  selectedPatientId.value = row.linkedPatientId || "";
  patientKeyword.value = row.patientName || "";
  bindDialogVisible.value = true;
  await searchPatients();
}

async function searchPatients() {
  candidatePatients.value = await fetchPatients(patientKeyword.value);
}

function selectPatient(row: Patient) {
  selectedPatientId.value = row.patientId;
}

async function handleBindPatient() {
  if (!selectedLead.value || !selectedPatientId.value) {
    return;
  }
  await bindPatientToWechatGroupLead(selectedLead.value.chatroomUsername, selectedPatientId.value);
  ElMessage.success("患者绑定成功");
  bindDialogVisible.value = false;
  await loadLeads();
}

async function handleUnbindPatient(row: WechatGroupLead) {
  await ElMessageBox.confirm(
    `确认解除线索“${row.rawGroupName || row.chatroomUsername}”当前绑定的患者吗？`,
    "解绑确认",
    { type: "warning" }
  );
  await unbindPatientFromWechatGroupLead(row.chatroomUsername);
  ElMessage.success("已解除患者绑定");
  await loadLeads();
}

onMounted(loadLeads);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.muted {
  color: #909399;
}

.bind-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bind-search {
  display: flex;
  gap: 12px;
}
</style>
