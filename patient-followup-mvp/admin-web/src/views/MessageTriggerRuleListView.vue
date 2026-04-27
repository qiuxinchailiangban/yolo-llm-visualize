<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div>
            <div class="toolbar-title">任务中心</div>
            <div class="toolbar-subtitle">定义流程任务、回复任务和其他可复用任务，并配置反馈规则</div>
          </div>
          <el-button type="primary" @click="openCreate">新增任务</el-button>
        </div>
      </template>

      <el-table :data="rules">
        <el-table-column prop="ruleName" label="任务名称" min-width="180" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <el-tag :type="taskCategoryTagType(row.taskCategory)">{{ taskCategoryLabel(row.taskCategory) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="90">
          <template #default="{ row }">
            <el-tag :type="row.ruleMode === 'AUTO' ? 'success' : 'warning'">
              {{ row.ruleMode === "AUTO" ? "自动" : "手动" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发条件" width="180">
          <template #default="{ row }">
            {{ triggerLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="发送对象" width="150">
          <template #default="{ row }">
            {{ targetLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="反馈规则" width="180">
          <template #default="{ row }">
            {{ feedbackRuleLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="内容摘要" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ contentSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastTriggeredAt" label="最近触发" width="180" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "开" : "关" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增任务定义' : '编辑任务定义'" width="820px" destroy-on-close>
      <div class="editor-shell">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">基础信息</div>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="任务名称" required>
              <el-input v-model="form.ruleName" placeholder="如：入群欢迎、术前提醒、关键词自动回复" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="任务分类" required>
                  <el-select v-model="form.taskCategory" style="width: 100%">
                    <el-option label="流程任务" value="PROCESS" />
                    <el-option label="回复任务" value="REPLY" />
                    <el-option label="其他任务" value="OTHER" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="执行模式" required>
                  <el-select v-model="form.ruleMode" style="width: 100%" :disabled="form.taskCategory === 'REPLY'">
                    <el-option label="自动" value="AUTO" />
                    <el-option label="手动" value="MANUAL" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否启用">
                  <el-switch v-model="form.enabled" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="排序">
                  <el-input-number v-model="form.sortOrder" :min="1" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="任务说明">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="2"
                resize="none"
                placeholder="可选，说明这个任务的用途和适用场景"
              />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-title">触发条件</div>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="触发方式" required>
              <el-select v-model="form.triggerType" style="width: 100%" :disabled="form.taskCategory === 'REPLY'">
                <el-option label="绑定微信群后立即发送" value="BIND_GROUP_IMMEDIATE" />
                <el-option label="相对手术日期发送" value="SURGERY_RELATIVE_DAY" />
                <el-option label="患者发送关键词消息后触发" value="KEYWORD_MESSAGE" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.triggerType === 'SURGERY_RELATIVE_DAY'" label="相对天数" required>
              <el-input-number v-model="form.relativeDayOffset" :step="1" />
              <span class="form-tip">负数表示术前，0 表示手术当天，正数表示术后</span>
            </el-form-item>
            <el-form-item v-if="form.triggerType === 'KEYWORD_MESSAGE'" label="关键词" required>
              <el-input
                v-model="form.keywordText"
                type="textarea"
                :rows="3"
                resize="none"
                placeholder="一行一个，或用逗号分隔；患者消息命中后触发"
              />
            </el-form-item>
            <el-form-item v-if="form.triggerType === 'KEYWORD_MESSAGE'" label="关键词匹配">
              <el-radio-group v-model="form.keywordMatchMode">
                <el-radio-button label="ANY">命中任一即可</el-radio-button>
                <el-radio-button label="ALL">全部命中才触发</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-title">反馈规则</div>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="需要反馈">
              <el-switch v-model="form.feedbackRequired" />
            </el-form-item>
            <template v-if="form.feedbackRequired">
              <el-form-item label="反馈判定">
                <el-select v-model="form.feedbackRule" style="width: 100%">
                  <el-option label="收到任意患者消息" value="ANY_MESSAGE" />
                  <el-option label="命中反馈关键词" value="KEYWORD" />
                  <el-option label="人工确认反馈" value="MANUAL_CONFIRM" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.feedbackRule === 'KEYWORD'" label="反馈关键词">
                <el-input
                  v-model="form.feedbackKeywordText"
                  type="textarea"
                  :rows="2"
                  resize="none"
                  placeholder="一行一个，或用逗号分隔；患者命中这些关键词视为已反馈"
                />
              </el-form-item>
              <el-form-item label="反馈超时">
                <el-input-number v-model="form.feedbackTimeoutHours" :min="1" />
                <span class="form-tip">超过该小时数仍无反馈，会进入异常中心</span>
              </el-form-item>
            </template>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <div class="section-title">复杂条件组合</div>
              <div class="section-actions">
                <el-button @click="addCondition">新增条件</el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="条件关系">
              <el-radio-group v-model="form.conditionRelation">
                <el-radio-button label="ALL">全部满足</el-radio-button>
                <el-radio-button label="ANY">任一满足</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>

          <div v-if="!form.conditions?.length" class="empty-blocks">当前未设置额外条件</div>

          <div v-for="(condition, index) in form.conditions" :key="`${condition.conditionType}-${index}`" class="content-block">
            <div class="content-block-header">
              <div class="content-block-title">条件 {{ index + 1 }}</div>
              <div class="content-block-actions">
                <el-button link @click="moveCondition(index, -1)" :disabled="index === 0">上移</el-button>
                <el-button link @click="moveCondition(index, 1)" :disabled="index === (form.conditions?.length ?? 0) - 1">下移</el-button>
                <el-button link type="danger" @click="removeCondition(index)">删除</el-button>
              </div>
            </div>
            <el-row :gutter="12">
              <el-col :span="10">
                <el-select v-model="condition.conditionType" style="width: 100%">
                  <el-option label="已绑定微信群" value="HAS_BOUND_CHATROOM" />
                  <el-option label="有手术日期" value="HAS_SURGERY_DATE" />
                  <el-option label="患者状态是" value="PATIENT_STATUS_IS" />
                  <el-option label="患者姓名包含" value="PATIENT_NAME_CONTAINS" />
                  <el-option label="诊断包含" value="DIAGNOSIS_CONTAINS" />
                  <el-option label="消息方向是" value="MESSAGE_DIRECTION_IS" />
                  <el-option label="消息内容包含" value="MESSAGE_CONTENT_CONTAINS" />
                </el-select>
              </el-col>
              <el-col :span="14">
                <el-input
                  v-model="condition.conditionValue"
                  :placeholder="conditionPlaceholder(condition.conditionType)"
                />
              </el-col>
            </el-row>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-title">发送对象</div>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="默认发送给" required>
              <el-select v-model="form.targetType" style="width: 100%">
                <el-option label="患者已绑定微信群" value="BOUND_CHATROOM" />
                <el-option label="患者姓名会话" value="PATIENT_NAME" />
                <el-option label="固定自定义会话" value="CUSTOM_CONVERSATION" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.targetType === 'CUSTOM_CONVERSATION'" label="固定会话名" required>
              <el-input v-model="form.customTargetConversation" placeholder="请输入固定的微信会话名称" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <div class="section-title">发送内容模块</div>
              <div class="section-actions">
                <el-button @click="addTextBlock">新增文本</el-button>
                <el-button @click="addImageBlock">新增图片</el-button>
              </div>
            </div>
          </template>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="支持多个文本模块 + 多张图片模块；发送时会按顺序合并文本，并在文字后按模块顺序串行发送图片。"
          />

          <div v-if="!form.contentBlocks.length" class="empty-blocks">
            请先添加文本或图片模块
          </div>

          <div v-for="(block, index) in form.contentBlocks" :key="`${block.blockType}-${index}`" class="content-block">
            <div class="content-block-header">
              <div class="content-block-title">
                {{ block.blockType === "TEXT" ? `文本模块 ${index + 1}` : `图片模块 ${index + 1}` }}
              </div>
              <div class="content-block-actions">
                <el-button link @click="moveBlock(index, -1)" :disabled="index === 0">上移</el-button>
                <el-button link @click="moveBlock(index, 1)" :disabled="index === form.contentBlocks.length - 1">下移</el-button>
                <el-button link type="danger" @click="removeBlock(index)">删除</el-button>
              </div>
            </div>

            <el-input
              v-if="block.blockType === 'TEXT'"
              v-model="block.textContent"
              type="textarea"
              :rows="4"
              resize="none"
              placeholder="请输入固定发送文案，支持 {{patientName}} / {{patientId}} / {{surgeryDate}} / {{groupName}} 变量"
            />

            <div v-else class="image-block">
              <div class="image-block-meta">
                <span>{{ block.mediaName || "暂未上传图片" }}</span>
              </div>
              <div class="image-block-actions">
                <el-button :loading="uploadingIndex === index" @click="selectImage(index)">
                  {{ block.mediaPath ? "重新上传" : "上传图片" }}
                </el-button>
                <el-button v-if="block.mediaPath" @click="clearImageBlock(block)">清空图片</el-button>
              </div>
              <input
                :ref="(el) => setFileInputRef(el as HTMLInputElement | null, index)"
                class="hidden-input"
                type="file"
                accept=".png,.jpg,.jpeg,.webp"
                @change="handleFileChange(index, $event)"
              />
            </div>
          </div>
        </el-card>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { onMounted, reactive, ref, watch } from "vue";
import {
  createMessageTriggerRule,
  deleteMessageTriggerRule,
  fetchMessageTriggerRules,
  updateMessageTriggerRule,
  uploadMessageTriggerRuleMedia,
} from "../api/messageTriggerRule";
import type {
  MessageTriggerRuleCondition,
  MessageTriggerRule,
  MessageTriggerRuleContentBlock,
} from "../types";

const rules = ref<MessageTriggerRule[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const saving = ref(false);
const uploadingIndex = ref<number | null>(null);
const editingId = ref<number>();
const fileInputRefs = ref<(HTMLInputElement | null)[]>([]);

const form = reactive<MessageTriggerRule>({
  ruleName: "",
  ruleMode: "AUTO",
  taskCategory: "PROCESS",
  triggerType: "BIND_GROUP_IMMEDIATE",
  relativeDayOffset: 0,
  keywordText: "",
  keywordMatchMode: "ANY",
  conditionRelation: "ALL",
  conditions: [],
  targetType: "BOUND_CHATROOM",
  customTargetConversation: "",
  contentBlocks: [],
  feedbackRequired: false,
  feedbackRule: "ANY_MESSAGE",
  feedbackKeywordText: "",
  feedbackTimeoutHours: 24,
  enabled: true,
  sortOrder: 100,
  description: "",
});

async function loadRules() {
  rules.value = await fetchMessageTriggerRules();
}

function openCreate() {
  dialogMode.value = "create";
  editingId.value = undefined;
  resetForm();
  addTextBlock();
  dialogVisible.value = true;
}

function openEdit(row: MessageTriggerRule) {
  dialogMode.value = "edit";
  editingId.value = row.id;
  Object.assign(form, {
    ruleCode: row.ruleCode,
    ruleName: row.ruleName,
    ruleMode: row.ruleMode,
    taskCategory: row.taskCategory ?? "PROCESS",
    triggerType: row.triggerType,
    relativeDayOffset: row.relativeDayOffset ?? 0,
    keywordText: row.keywordText ?? "",
    keywordMatchMode: row.keywordMatchMode ?? "ANY",
    conditionRelation: row.conditionRelation ?? "ALL",
    conditions: (row.conditions ?? []).map((item) => ({ ...item })),
    targetType: row.targetType,
    customTargetConversation: row.customTargetConversation ?? "",
    contentBlocks: row.contentBlocks.map((block) => ({ ...block })),
    feedbackRequired: row.feedbackRequired ?? false,
    feedbackRule: row.feedbackRule ?? "ANY_MESSAGE",
    feedbackKeywordText: row.feedbackKeywordText ?? "",
    feedbackTimeoutHours: row.feedbackTimeoutHours ?? 24,
    enabled: row.enabled,
    sortOrder: row.sortOrder,
    description: row.description ?? "",
  });
  dialogVisible.value = true;
}

function resetForm() {
  Object.assign(form, {
    ruleCode: "",
    ruleName: "",
    ruleMode: "AUTO",
    taskCategory: "PROCESS",
    triggerType: "BIND_GROUP_IMMEDIATE",
    relativeDayOffset: 0,
    keywordText: "",
    keywordMatchMode: "ANY",
    conditionRelation: "ALL",
    conditions: [] as MessageTriggerRuleCondition[],
    targetType: "BOUND_CHATROOM",
    customTargetConversation: "",
    contentBlocks: [] as MessageTriggerRuleContentBlock[],
    feedbackRequired: false,
    feedbackRule: "ANY_MESSAGE",
    feedbackKeywordText: "",
    feedbackTimeoutHours: 24,
    enabled: true,
    sortOrder: 100,
    description: "",
  });
  fileInputRefs.value = [];
}

function addTextBlock() {
  form.contentBlocks.push({
    blockType: "TEXT",
    textContent: "",
    sortOrder: form.contentBlocks.length + 1,
  });
}

function addImageBlock() {
  form.contentBlocks.push({
    blockType: "IMAGE",
    mediaPath: "",
    mediaName: "",
    sortOrder: form.contentBlocks.length + 1,
  });
}

function addCondition() {
  (form.conditions ||= []).push({
    conditionType: "HAS_BOUND_CHATROOM",
    conditionValue: "true",
    sortOrder: (form.conditions?.length ?? 0) + 1,
  });
}

function removeCondition(index: number) {
  form.conditions?.splice(index, 1);
  normalizeConditionSortOrder();
}

function moveCondition(index: number, offset: number) {
  const list = form.conditions || [];
  const target = index + offset;
  if (target < 0 || target >= list.length) {
    return;
  }
  const [item] = list.splice(index, 1);
  list.splice(target, 0, item);
  normalizeConditionSortOrder();
}

function removeBlock(index: number) {
  form.contentBlocks.splice(index, 1);
  normalizeBlockSortOrder();
}

function moveBlock(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= form.contentBlocks.length) {
    return;
  }
  const [item] = form.contentBlocks.splice(index, 1);
  form.contentBlocks.splice(target, 0, item);
  normalizeBlockSortOrder();
}

function clearImageBlock(block: MessageTriggerRuleContentBlock) {
  block.mediaPath = "";
  block.mediaName = "";
}

function normalizeBlockSortOrder() {
  form.contentBlocks.forEach((block, index) => {
    block.sortOrder = index + 1;
  });
}

function normalizeConditionSortOrder() {
  (form.conditions || []).forEach((condition, index) => {
    condition.sortOrder = index + 1;
  });
}

function setFileInputRef(el: HTMLInputElement | null, index: number) {
  fileInputRefs.value[index] = el;
}

function selectImage(index: number) {
  fileInputRefs.value[index]?.click();
}

async function handleFileChange(index: number, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  uploadingIndex.value = index;
  try {
    const result = await uploadMessageTriggerRuleMedia(file);
    form.contentBlocks[index].mediaPath = result.mediaPath;
    form.contentBlocks[index].mediaName = result.mediaName;
    ElMessage.success("图片上传成功");
  } finally {
    uploadingIndex.value = null;
    input.value = "";
  }
}

function triggerLabel(rule: MessageTriggerRule) {
  if (rule.triggerType === "BIND_GROUP_IMMEDIATE") {
    return "绑定群后立即发送";
  }
  if (rule.triggerType === "KEYWORD_MESSAGE") {
    return "患者关键词消息触发";
  }
  const day = rule.relativeDayOffset ?? 0;
  if (day === 0) {
    return "手术当天发送";
  }
  return day > 0 ? `术后第 ${day} 天发送` : `术前 ${Math.abs(day)} 天发送`;
}

function targetLabel(rule: MessageTriggerRule) {
  if (rule.targetType === "BOUND_CHATROOM") {
    return "绑定微信群";
  }
  if (rule.targetType === "PATIENT_NAME") {
    return "患者姓名会话";
  }
  return rule.customTargetConversation || "固定会话";
}

function taskCategoryLabel(category?: MessageTriggerRule["taskCategory"]) {
  if (category === "REPLY") return "回复任务";
  if (category === "OTHER") return "其他任务";
  return "流程任务";
}

function taskCategoryTagType(category?: MessageTriggerRule["taskCategory"]): "success" | "warning" | "info" {
  if (category === "REPLY") return "warning";
  if (category === "OTHER") return "info";
  return "success";
}

function feedbackRuleLabel(rule: MessageTriggerRule) {
  if (!rule.feedbackRequired) {
    return "无需反馈";
  }
  if (rule.feedbackRule === "KEYWORD") {
    return "关键词反馈";
  }
  if (rule.feedbackRule === "MANUAL_CONFIRM") {
    return "人工确认";
  }
  return "任意消息";
}

function contentSummary(rule: MessageTriggerRule) {
  const textPreview = rule.contentBlocks
    .filter((item) => item.blockType === "TEXT" && item.textContent?.trim())
    .map((item) => item.textContent?.trim())
    .join(" / ");
  const imageCount = rule.contentBlocks.filter((item) => item.blockType === "IMAGE" && item.mediaPath).length;
  const textPart = textPreview || "无文本";
  return imageCount > 0 ? `${textPart} + 图片 x${imageCount}` : textPart;
}

function conditionPlaceholder(conditionType: MessageTriggerRuleCondition["conditionType"]) {
  switch (conditionType) {
    case "HAS_BOUND_CHATROOM":
    case "HAS_SURGERY_DATE":
      return "true / false";
    case "PATIENT_STATUS_IS":
      return "如 FOLLOWING / ACTIVE";
    case "PATIENT_NAME_CONTAINS":
      return "姓名关键字";
    case "DIAGNOSIS_CONTAINS":
      return "诊断关键字";
    case "MESSAGE_DIRECTION_IS":
      return "INBOUND / OUTBOUND";
    case "MESSAGE_CONTENT_CONTAINS":
      return "消息内容关键字";
    default:
      return "请输入条件值";
  }
}

function buildPayload(): MessageTriggerRule {
  normalizeBlockSortOrder();
  return {
    ...form,
    ruleName: form.ruleName.trim(),
    taskCategory: form.taskCategory,
    ruleMode: form.taskCategory === "REPLY" ? "AUTO" : form.ruleMode,
    triggerType: form.taskCategory === "REPLY" ? "KEYWORD_MESSAGE" : form.triggerType,
    keywordText: form.triggerType === "KEYWORD_MESSAGE" || form.taskCategory === "REPLY" ? form.keywordText?.trim() : "",
    customTargetConversation: form.targetType === "CUSTOM_CONVERSATION"
      ? form.customTargetConversation?.trim()
      : "",
    relativeDayOffset: form.triggerType === "SURGERY_RELATIVE_DAY" ? form.relativeDayOffset ?? 0 : undefined,
    feedbackRequired: form.feedbackRequired ?? false,
    feedbackRule: form.feedbackRequired ? form.feedbackRule : "NONE",
    feedbackKeywordText: form.feedbackRequired && form.feedbackRule === "KEYWORD"
      ? form.feedbackKeywordText?.trim()
      : "",
    feedbackTimeoutHours: form.feedbackRequired ? form.feedbackTimeoutHours ?? 24 : undefined,
    conditions: (form.conditions || []).map((condition) => ({
      conditionType: condition.conditionType,
      conditionValue: condition.conditionValue?.trim(),
      sortOrder: condition.sortOrder,
    })),
    contentBlocks: form.contentBlocks.map((block) => ({
      blockType: block.blockType,
      textContent: block.blockType === "TEXT" ? block.textContent?.trim() : "",
      mediaPath: block.blockType === "IMAGE" ? block.mediaPath : "",
      mediaName: block.blockType === "IMAGE" ? block.mediaName : "",
      sortOrder: block.sortOrder,
    })),
  };
}

async function submitForm() {
  if (!form.ruleName.trim()) {
    ElMessage.warning("请填写任务名称");
    return;
  }
  if (form.triggerType === "SURGERY_RELATIVE_DAY" && form.relativeDayOffset === undefined) {
    ElMessage.warning("请填写相对天数");
    return;
  }
  if ((form.triggerType === "KEYWORD_MESSAGE" || form.taskCategory === "REPLY") && !form.keywordText?.trim()) {
    ElMessage.warning("请填写关键词");
    return;
  }
  if (form.feedbackRequired && form.feedbackRule === "KEYWORD" && !form.feedbackKeywordText?.trim()) {
    ElMessage.warning("请填写反馈关键词");
    return;
  }
  if (form.targetType === "CUSTOM_CONVERSATION" && !form.customTargetConversation?.trim()) {
    ElMessage.warning("请填写固定会话名");
    return;
  }
  if (!form.contentBlocks.length) {
    ElMessage.warning("请至少添加一个内容模块");
    return;
  }

  saving.value = true;
  try {
    const payload = buildPayload();
    if (dialogMode.value === "create") {
      await createMessageTriggerRule(payload);
      ElMessage.success("任务定义创建成功");
    } else if (editingId.value) {
      await updateMessageTriggerRule(editingId.value, payload);
      ElMessage.success("任务定义更新成功");
    }
    dialogVisible.value = false;
    await loadRules();
  } finally {
    saving.value = false;
  }
}

async function confirmDelete(row: MessageTriggerRule) {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除任务“${row.ruleName}”吗？`, "删除任务定义", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  await deleteMessageTriggerRule(row.id);
  ElMessage.success("任务定义已删除");
  await loadRules();
}

watch(
  () => form.taskCategory,
  (category) => {
    if (category === "REPLY") {
      form.ruleMode = "AUTO";
      form.triggerType = "KEYWORD_MESSAGE";
      form.feedbackRequired = false;
      form.feedbackRule = "NONE";
      form.feedbackKeywordText = "";
    }
  }
);

watch(
  () => form.feedbackRequired,
  (enabled) => {
    if (!enabled) {
      form.feedbackRule = "NONE";
      form.feedbackKeywordText = "";
    } else if (form.feedbackRule === "NONE") {
      form.feedbackRule = "ANY_MESSAGE";
    }
  }
);

onMounted(loadRules);
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
  gap: 16px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 600;
}

.toolbar-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.editor-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.section-actions {
  display: flex;
  gap: 12px;
}

.form-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.empty-blocks {
  padding: 16px 0 4px;
  color: #909399;
}

.content-block {
  margin-top: 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  background: #fafbfd;
}

.content-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.content-block-title {
  font-weight: 600;
}

.content-block-actions {
  display: flex;
  gap: 8px;
}

.image-block {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  min-height: 48px;
}

.image-block-meta {
  color: #606266;
  word-break: break-all;
}

.image-block-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.hidden-input {
  display: none;
}
</style>
