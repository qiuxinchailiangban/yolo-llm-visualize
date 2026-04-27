<template>
  <div class="page template-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <div>
            <div class="toolbar-title">流程模板</div>
              <div class="toolbar-subtitle">把任务中心里定义好的流程任务装配成围手术期模板</div>
          </div>
          <div class="toolbar-actions">
            <el-button @click="goDashboard">返回流程总览</el-button>
            <el-button type="primary" @click="openCreate">新增模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="templates">
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="templateCategory" label="分类" width="120" />
        <el-table-column prop="stepCount" label="节点数" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认模板" width="120">
          <template #default="{ row }">
            <el-tag :type="row.defaultTemplate ? 'success' : 'info'">{{ row.defaultTemplate ? "默认" : "否" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增流程模板' : '编辑流程模板'" width="1100px" destroy-on-close>
      <div class="editor-grid">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">模板信息</div>
          </template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="模板名称" required>
              <el-input v-model="form.templateName" placeholder="如：膝关节置换围手术期流程" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="模板分类">
                  <el-input v-model="form.templateCategory" placeholder="如：围手术期 / 日间手术" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="默认模板">
                  <el-switch v-model="form.defaultTemplate" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否启用">
                  <el-switch v-model="form.active" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="模板说明">
              <el-input v-model="form.description" type="textarea" :rows="3" resize="none" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <div class="section-title">流程节点</div>
              <el-button @click="addStep">新增节点</el-button>
            </div>
          </template>

          <div v-if="!form.steps.length" class="empty-state">当前还没有节点，请先新增一个节点</div>

          <div v-for="(step, index) in form.steps" :key="`${step.stepCode || 'step'}-${index}`" class="step-card">
            <div class="step-card-header">
              <div>
                <div class="step-card-title">节点 {{ index + 1 }} · {{ step.stepName || "未命名节点" }}</div>
                <div class="step-card-subtitle">{{ step.stepType === "QUESTIONNAIRE" ? "问卷节点" : "消息节点" }}</div>
              </div>
              <div class="step-card-actions">
                <el-button link @click="moveStep(index, -1)" :disabled="index === 0">上移</el-button>
                <el-button link @click="moveStep(index, 1)" :disabled="index === form.steps.length - 1">下移</el-button>
                <el-button link type="danger" @click="removeStep(index)">删除</el-button>
              </div>
            </div>

            <el-row :gutter="12">
              <el-col :span="10">
                <el-form-item label="节点名称" label-width="86px">
                  <el-input v-model="step.stepName" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="节点类型" label-width="86px">
                  <el-select v-model="step.stepType" style="width: 100%">
                    <el-option label="消息节点" value="MESSAGE" />
                    <el-option label="问卷节点" value="QUESTIONNAIRE" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="触发方式" label-width="86px">
                  <el-select v-model="step.triggerMode" style="width: 100%">
                    <el-option label="绑定群后触发" value="EVENT_BIND_GROUP" />
                    <el-option label="相对手术日触发" value="SURGERY_RELATIVE" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="相对天数" label-width="86px">
                  <el-input-number v-model="step.relativeDayOffset" :step="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col v-if="step.stepType === 'MESSAGE'" :span="8">
                <el-form-item label="关联任务" label-width="86px">
                  <el-select
                    v-model="step.messageRuleCode"
                    filterable
                    clearable
                    style="width: 100%"
                    placeholder="选择任务中心里的流程任务"
                    @change="handleTaskChange(step)"
                  >
                    <el-option
                      v-for="task in availableProcessTasks"
                      :key="task.ruleCode"
                      :label="task.ruleName"
                      :value="task.ruleCode"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="step.stepType === 'MESSAGE'" :span="8">
                <el-form-item label="任务反馈" label-width="86px">
                  <el-input :model-value="taskFeedbackLabel(step)" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="阶段编码" label-width="86px">
                  <el-input v-model="step.stageCode" placeholder="可填 stageCode" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="模板编码" label-width="86px">
                  <el-input v-model="step.templateCode" placeholder="可填 templateCode" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="适用场次" label-width="86px">
                  <el-input v-model="step.applicableSurgeryTags" placeholder="如：第1台,第2台" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="需反馈" label-width="86px">
                  <el-switch v-model="step.feedbackRequired" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="启用" label-width="86px">
                  <el-switch v-model="step.enabled" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="节点说明" label-width="86px">
                  <el-input v-model="step.description" type="textarea" :rows="2" resize="none" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存模板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { createPatientProcessTemplate, fetchPatientProcessTemplates, updatePatientProcessTemplate } from "../api/patientProcess";
import { fetchMessageTriggerRules } from "../api/messageTriggerRule";
import type { MessageTriggerRule, PatientProcessTemplate, PatientProcessTemplateStep } from "../types";

const router = useRouter();
const templates = ref<PatientProcessTemplate[]>([]);
const tasks = ref<MessageTriggerRule[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const saving = ref(false);
const editingId = ref<number>();

const form = reactive<PatientProcessTemplate>({
  templateName: "",
  templateCategory: "围手术期",
  description: "",
  active: true,
  defaultTemplate: false,
  steps: [],
});

const availableProcessTasks = computed(() =>
  tasks.value.filter((task) => task.taskCategory !== "REPLY"),
);

async function loadTemplates() {
  templates.value = await fetchPatientProcessTemplates();
}

async function loadTasks() {
  tasks.value = await fetchMessageTriggerRules();
}

function goDashboard() {
  router.push("/patient-processes");
}

function openCreate() {
  dialogMode.value = "create";
  editingId.value = undefined;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(template: PatientProcessTemplate) {
  dialogMode.value = "edit";
  editingId.value = template.id;
  Object.assign(form, {
    id: template.id,
    templateCode: template.templateCode,
    templateName: template.templateName,
    templateCategory: template.templateCategory ?? "",
    description: template.description ?? "",
    active: template.active,
    defaultTemplate: template.defaultTemplate ?? false,
    builtIn: template.builtIn,
    steps: (template.steps ?? []).map((step) => ({
      ...step,
      messageRuleName: resolveTaskName(step.messageRuleCode),
    })),
  });
  dialogVisible.value = true;
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    templateCode: undefined,
    templateName: "",
    templateCategory: "围手术期",
    description: "",
    active: true,
    defaultTemplate: false,
    builtIn: false,
    steps: [],
  });
}

function addStep() {
  form.steps.push({
    stepName: "",
    sortOrder: form.steps.length + 1,
    stepType: "MESSAGE",
    triggerMode: "SURGERY_RELATIVE",
    relativeDayOffset: 0,
    feedbackRequired: false,
    enabled: true,
  });
}

function resolveTaskName(ruleCode?: string) {
  return tasks.value.find((task) => task.ruleCode === ruleCode)?.ruleName;
}

function findTask(ruleCode?: string) {
  return tasks.value.find((task) => task.ruleCode === ruleCode);
}

function handleTaskChange(step: PatientProcessTemplateStep) {
  const task = findTask(step.messageRuleCode);
  step.messageRuleName = task?.ruleName;
  if (!task) {
    return;
  }
  if (!step.stepName?.trim()) {
    step.stepName = task.ruleName;
  }
  if (!step.description?.trim() && task.description?.trim()) {
    step.description = task.description;
  }
  step.feedbackRequired = task.feedbackRequired ?? false;
}

function taskFeedbackLabel(step: PatientProcessTemplateStep) {
  const task = findTask(step.messageRuleCode);
  if (!task?.feedbackRequired) {
    return "无需反馈";
  }
  if (task.feedbackRule === "KEYWORD") {
    return "关键词反馈";
  }
  if (task.feedbackRule === "MANUAL_CONFIRM") {
    return "人工确认";
  }
  return "任意消息反馈";
}

function removeStep(index: number) {
  form.steps.splice(index, 1);
  normalizeSortOrder();
}

function moveStep(index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= form.steps.length) {
    return;
  }
  const [item] = form.steps.splice(index, 1);
  form.steps.splice(targetIndex, 0, item);
  normalizeSortOrder();
}

function normalizeSortOrder() {
  form.steps = form.steps.map((step, index) => ({ ...step, sortOrder: index + 1 }));
}

async function submitForm() {
  if (!form.templateName.trim()) {
    ElMessage.warning("请先填写模板名称");
    return;
  }
  if (!form.steps.length) {
    ElMessage.warning("请至少配置一个流程节点");
    return;
  }
  saving.value = true;
  try {
    const payload: PatientProcessTemplate = {
      templateCode: form.templateCode,
      templateName: form.templateName.trim(),
      templateCategory: form.templateCategory?.trim(),
      description: form.description?.trim(),
      active: form.active,
      defaultTemplate: form.defaultTemplate,
      steps: form.steps.map((step, index) => normalizeStep(step, index)),
    };
    if (dialogMode.value === "create") {
      await createPatientProcessTemplate(payload);
      ElMessage.success("流程模板创建成功");
    } else if (editingId.value) {
      await updatePatientProcessTemplate(editingId.value, payload);
      ElMessage.success("流程模板更新成功");
    }
    dialogVisible.value = false;
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

function normalizeStep(step: PatientProcessTemplateStep, index: number): PatientProcessTemplateStep {
  return {
    stepCode: step.stepCode?.trim(),
    stepName: step.stepName.trim(),
    sortOrder: index + 1,
    stepType: step.stepType,
    triggerMode: step.triggerMode,
    relativeDayOffset: step.relativeDayOffset ?? 0,
    description: step.description?.trim(),
    messageRuleCode: step.messageRuleCode?.trim(),
    stageCode: step.stageCode?.trim(),
    templateCode: step.templateCode?.trim(),
    completionRule: step.completionRule?.trim(),
    applicableSurgeryTags: step.applicableSurgeryTags?.trim(),
    feedbackRequired: step.feedbackRequired ?? false,
    enabled: step.enabled ?? true,
  };
}

onMounted(async () => {
  await Promise.all([loadTemplates(), loadTasks()]);
});
</script>

<style scoped>
.template-page {
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.toolbar-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.editor-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.empty-state {
  padding: 28px 12px;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  text-align: center;
}

.step-card {
  margin-top: 14px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}

.step-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.step-card-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.step-card-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.step-card-actions {
  display: flex;
  gap: 8px;
}
</style>
