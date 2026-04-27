<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span>随访阶段管理</span>
          <div class="toolbar-actions">
            <el-button :loading="rebuildLoading" @click="confirmRebuildAllTasks">重建所有患者任务</el-button>
            <el-button type="primary" @click="openCreate">新增阶段</el-button>
          </div>
        </div>
      </template>

      <el-table :data="stages">
        <el-table-column prop="stageCode" label="阶段编码" width="160" />
        <el-table-column prop="stageName" label="阶段名称" width="140" />
        <el-table-column prop="dayOffset" label="相对手术日" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "是" : "否" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提醒" width="90">
          <template #default="{ row }">
            <el-tag :type="row.reminderEnabled ? 'warning' : 'info'">{{ row.reminderEnabled ? "开" : "关" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑阶段' : '新增阶段'" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="阶段编码"><el-input v-model="form.stageCode" /></el-form-item>
        <el-form-item label="阶段名称"><el-input v-model="form.stageName" /></el-form-item>
        <el-form-item label="相对手术日"><el-input-number v-model="form.dayOffset" :step="1" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" /></el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="提醒开关"><el-switch v-model="form.reminderEnabled" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { onMounted, reactive, ref } from "vue";
import { rebuildAllPatientTasks } from "../api/patient";
import { createStage, fetchStages, updateStage } from "../api/stage";
import type { Stage, StageForm } from "../types";

const stages = ref<Stage[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number>();
const rebuildLoading = ref(false);
const form = reactive<StageForm>({
  stageCode: "",
  stageName: "",
  dayOffset: 0,
  sortOrder: 10,
  enabled: true,
  reminderEnabled: true,
  description: "",
});

async function loadStages() {
  stages.value = await fetchStages();
}

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, {
    stageCode: "",
    stageName: "",
    dayOffset: 0,
    sortOrder: 10,
    enabled: true,
    reminderEnabled: true,
    description: "",
  });
  dialogVisible.value = true;
}

function openEdit(row: Stage) {
  editingId.value = row.id;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submitForm() {
  if (editingId.value) {
    await updateStage(editingId.value, form);
    ElMessage.success("阶段更新成功");
  } else {
    await createStage(form);
    ElMessage.success("阶段创建成功");
  }
  dialogVisible.value = false;
  await loadStages();
}

async function confirmRebuildAllTasks() {
  try {
    await ElMessageBox.confirm(
      "确认按当前启用阶段和模板，重建所有患者的随访任务吗？未完成任务可能会被更新或取消。",
      "重建所有患者任务",
      {
        confirmButtonText: "开始重建",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  } catch {
    return;
  }

  rebuildLoading.value = true;
  try {
    const result = await rebuildAllPatientTasks();
    ElMessage.success(
      `重建完成：共 ${result.totalPatients} 位患者，处理 ${result.rebuiltPatients} 位，跳过 ${result.skippedPatients} 位，影响 ${result.totalTasksAffected} 条任务`,
    );
  } finally {
    rebuildLoading.value = false;
  }
}

onMounted(loadStages);
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

.toolbar-actions {
  display: flex;
  gap: 12px;
}
</style>
