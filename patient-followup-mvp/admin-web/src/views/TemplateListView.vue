<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div>
            <div class="toolbar-title">问卷模板管理</div>
            <div class="toolbar-subtitle">支持选择题、填空题、评分题、矩阵题等多题型设计</div>
          </div>
          <el-button type="primary" @click="openCreate">新增模板</el-button>
        </div>
      </template>

      <el-table :data="templates">
        <el-table-column prop="templateCode" label="模板编码" width="150" />
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="templateType" label="类型" width="120" />
        <el-table-column prop="stageName" label="阶段" width="140" />
        <el-table-column label="题目数" width="90">
          <template #default="{ row }">
            {{ getQuestionCount(row.schemaJson) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-tooltip
              v-if="row.id"
              :disabled="canExportQr(row)"
              :content="qrDisabledReason(row)"
              placement="top"
            >
              <span>
                <el-button
                  link
                  type="success"
                  :disabled="!canExportQr(row)"
                  @click="openTemplateQrDialog(row)"
                >
                  导出二维码
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="qrDialogVisible" title="问卷二维码" width="560px" destroy-on-close>
      <div v-if="currentQrCode" class="qr-dialog">
        <div class="qr-meta">
          <div class="qr-title">{{ currentQrCode.targetName }}</div>
          <div class="qr-subtitle">{{ currentQrCode.targetCode }}</div>
        </div>

        <div class="qr-image-wrap">
          <img v-if="qrImageUrl" :src="qrImageUrl" alt="问卷二维码" class="qr-image" />
          <div v-else-if="qrImageError" class="qr-image-error">
            <div class="qr-image-error-text">{{ qrImageError }}</div>
            <el-button type="primary" plain :loading="qrImageLoading" @click="loadQrImage">重试</el-button>
            <div class="qr-image-error-hint">
              如果一直失败，去看后端日志里 <code>WechatMiniappService</code> 的报错（常见：体验版小程序还没把
              <code>pages/questionnaire/index</code> 上传，或 appSecret 写错）。
            </div>
          </div>
          <el-skeleton v-else :rows="6" animated />
        </div>

        <el-alert
          :title="currentQrCode.wechatConfigured ? '当前显示的是微信小程序码，患者可直接微信扫码进入问卷。' : '当前显示的是调试二维码。接入微信 appSecret 后会自动切换成官方小程序码。'"
          :type="currentQrCode.wechatConfigured ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />

        <el-alert
          v-if="isFollowUpSharedQr"
          type="info"
          :closable="false"
          show-icon
          title="共享随访码使用说明"
          description="同一阶段所有患者都用这一个码。患者扫码后需在问卷开头填写【姓名 + 手机号】，后端按手机号匹配患者名单并自动定位到 ta 的待填任务。手机号或姓名对不上会被拒收。"
        />

        <el-descriptions :column="1" border class="qr-desc">
          <el-descriptions-item label="小程序页面">{{ currentQrCode.pagePath }}</el-descriptions-item>
          <el-descriptions-item label="扫码 token">{{ currentQrCode.token }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ currentQrCode.expiresAt }}</el-descriptions-item>
          <el-descriptions-item label="调试链接">{{ currentQrCode.debugUrl }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="qrDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!qrImageUrl" @click="downloadQrImage">下载图片</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="basicInfoDialogVisible" title="新建问卷模板" width="720px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="模板编码" required>
          <el-input v-model="form.templateCode" placeholder="如 PRE_OP_V2" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="请输入问卷名称" @input="syncSchemaTitle" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模板类型" required>
              <el-select v-model="form.templateType" style="width: 100%">
                <el-option label="首诊问卷" value="INTAKE" />
                <el-option label="随访问卷" value="FOLLOW_UP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访阶段">
              <el-select v-model="form.stageId" clearable style="width: 100%" :disabled="form.templateType === 'INTAKE'">
                <el-option v-for="item in stages" :key="item.id" :label="item.stageName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="版本" required>
              <el-input v-model="form.version" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" required>
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入问卷用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="basicInfoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="goDesignerFromBasicInfo">下一步：设计问卷</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="metaDialogVisible" title="编辑模板基本信息" width="720px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="模板编码" required>
          <el-input v-model="form.templateCode" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" @input="syncSchemaTitle" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模板类型" required>
              <el-select v-model="form.templateType" style="width: 100%">
                <el-option label="首诊问卷" value="INTAKE" />
                <el-option label="随访问卷" value="FOLLOW_UP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访阶段">
              <el-select v-model="form.stageId" clearable style="width: 100%" :disabled="form.templateType === 'INTAKE'">
                <el-option v-for="item in stages" :key="item.id" :label="item.stageName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="版本" required>
              <el-input v-model="form.version" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" required>
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metaDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="metaDialogVisible = false">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="designerVisible" fullscreen destroy-on-close class="designer-dialog">
      <template #header>
        <div class="designer-header">
          <div>
            <div class="designer-title">{{ editingId ? "编辑问卷模板" : "创建问卷模板" }}</div>
            <div class="designer-subtitle">
              {{ form.templateName || "未命名问卷" }}
              <span v-if="currentStageName"> / {{ currentStageName }}</span>
              <span> / {{ form.version }}</span>
            </div>
          </div>
          <div class="designer-header-actions">
            <el-button @click="metaDialogVisible = true">基本信息</el-button>
            <el-button @click="designerVisible = false">关闭</el-button>
            <el-button type="primary" @click="submitForm">保存模板</el-button>
          </div>
        </div>
      </template>

      <div class="designer-shell">
        <div class="designer-body">
          <div class="toolbox-side">
            <div class="toolbox-bar">
              <div class="toolbox-title">题型库</div>
              <div class="toolbox-groups">
                <div v-for="category in questionCategories" :key="category.key" class="toolbox-group">
                  <div class="toolbox-group-title">{{ category.label }}</div>
                  <div class="toolbox-buttons">
                    <el-button
                      v-for="item in questionTypeDefinitions.filter((question) => question.category === category.key)"
                      :key="item.type"
                      class="toolbox-button"
                      @click="addQuestion(item.type)"
                    >
                      {{ item.label }}
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="canvas-wrap">
            <div class="survey-cover">
              <div class="survey-cover-title">
                <el-input
                  v-model="schema.title"
                  class="cover-title-input"
                  placeholder="问卷标题"
                />
              </div>
              <div class="survey-cover-desc">
                <el-input
                  v-model="schema.description"
                  type="textarea"
                  :rows="3"
                  resize="none"
                  placeholder="请输入问卷说明，让患者了解填写目的与注意事项"
                />
              </div>
            </div>

            <el-empty v-if="schema.items.length === 0" description="请从左侧题型库添加题目" class="empty-block" />

            <Draggable
              v-model="schema.items"
              item-key="id"
              handle=".drag-handle"
              animation="200"
              ghost-class="dragging-ghost"
              chosen-class="dragging-chosen"
            >
              <template #item="{ element, index }">
                <QuestionEditorCard
                  :question="element"
                  :index="index"
                  @remove="removeQuestion(index)"
                  @duplicate="duplicateCurrentQuestion(element, index)"
                  @move-up="moveQuestion(index, -1)"
                  @move-down="moveQuestion(index, 1)"
                />
              </template>
            </Draggable>
          </div>

          <div class="designer-side">
            <el-card shadow="never">
              <template #header>
                <div class="panel-title">问卷预览</div>
              </template>
              <div class="preview-panel">
                <div class="preview-title">{{ schema.title || form.templateName || "未命名问卷" }}</div>
                <div v-if="schema.description" class="preview-description">{{ schema.description }}</div>
                <QuestionPreviewRenderer
                  v-for="(question, index) in schema.items"
                  :key="question.id"
                  :question="question"
                  :index="index"
                />
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";
import Draggable from "vuedraggable";
import QuestionEditorCard from "../components/template/QuestionEditorCard.vue";
import QuestionPreviewRenderer from "../components/template/QuestionPreviewRenderer.vue";
import { createTemplateQrCode, fetchQrCodeImage } from "../api/qrcode";
import { fetchStages } from "../api/stage";
import { createTemplate, fetchTemplates, updateTemplate } from "../api/template";
import type { QrCodeInfo, QuestionType, Stage, SurveySchema, Template } from "../types";
import {
  createEmptySchema,
  createQuestionByType,
  duplicateQuestion,
  getQuestionTypeDefinitions,
  parseSchemaJson,
  questionCategories,
  stringifySchema,
} from "../utils/templateDesigner";

const templates = ref<Template[]>([]);
const stages = ref<Stage[]>([]);
const basicInfoDialogVisible = ref(false);
const metaDialogVisible = ref(false);
const designerVisible = ref(false);
const qrDialogVisible = ref(false);
const editingId = ref<number>();
const currentQrCode = ref<QrCodeInfo>();
const currentQrTemplate = ref<Template | null>(null);
const qrImageUrl = ref("");
const qrImageError = ref("");
const qrImageLoading = ref(false);

const isFollowUpSharedQr = computed(() => {
  return Boolean(
    currentQrCode.value &&
      currentQrTemplate.value &&
      currentQrTemplate.value.templateType === "FOLLOW_UP"
  );
});
const form = reactive<Template>({
  templateCode: "",
  templateName: "",
  templateType: "FOLLOW_UP",
  version: "v1",
  stageId: undefined,
  status: "DRAFT",
  schemaJson: '{"title":"新问卷","items":[]}',
  description: "",
});
const schema = ref<SurveySchema>(createEmptySchema());

const questionTypeDefinitions = getQuestionTypeDefinitions();
const statusTagType: Record<string, string> = {
  DRAFT: "info",
  ACTIVE: "success",
  DISABLED: "warning",
};

const currentStageName = computed(() => stages.value.find((item) => item.id === form.stageId)?.stageName || "");

async function loadData() {
  templates.value = await fetchTemplates();
  stages.value = await fetchStages();
}

function resetForm() {
  Object.assign(form, {
    templateCode: "",
    templateName: "",
    templateType: "FOLLOW_UP",
    version: "v1",
    stageId: undefined,
    status: "DRAFT",
    schemaJson: '{"title":"新问卷","items":[]}',
    description: "",
  });
  schema.value = createEmptySchema("新问卷");
}

function openCreate() {
  editingId.value = undefined;
  resetForm();
  basicInfoDialogVisible.value = true;
}

async function openTemplateQrDialog(row: Template) {
  if (!row.id) {
    return;
  }
  if (qrImageUrl.value) {
    URL.revokeObjectURL(qrImageUrl.value);
  }
  qrImageUrl.value = "";
  qrImageError.value = "";
  currentQrCode.value = undefined;
  currentQrTemplate.value = row;
  qrDialogVisible.value = true;
  try {
    currentQrCode.value = await createTemplateQrCode(row.id);
  } catch (error) {
    qrDialogVisible.value = false;
    ElMessage.error(extractErrorMessage(error, "创建二维码失败"));
    return;
  }
  await loadQrImage();
}

function openEdit(row: Template) {
  editingId.value = row.id;
  Object.assign(form, row);
  if (form.templateType === "INTAKE") {
    form.stageId = undefined;
  }
  try {
    schema.value = parseSchemaJson(row.schemaJson, row.templateName || "问卷");
  } catch (error) {
    ElMessage.warning("旧模板 schema 解析失败，已按空白问卷载入");
    schema.value = createEmptySchema(row.templateName || "问卷");
  }
  designerVisible.value = true;
}

function goDesignerFromBasicInfo() {
  if (!validateBasicInfo()) {
    return;
  }
  basicInfoDialogVisible.value = false;
  designerVisible.value = true;
}

function addQuestion(type: QuestionType) {
  schema.value.items.push(createQuestionByType(type, schema.value.items.length));
}

function removeQuestion(index: number) {
  schema.value.items.splice(index, 1);
}

function moveQuestion(index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= schema.value.items.length) {
    return;
  }
  const [current] = schema.value.items.splice(index, 1);
  schema.value.items.splice(targetIndex, 0, current);
}

function duplicateCurrentQuestion(question: SurveyQuestionLike, index: number) {
  schema.value.items.splice(index + 1, 0, duplicateQuestion(question, index + 1));
}

function syncSchemaTitle() {
  if (!schema.value.title || schema.value.title === "新问卷") {
    schema.value.title = form.templateName || "新问卷";
  }
}

async function submitForm() {
  if (!validateBasicInfo()) {
    return;
  }
  if (schema.value.items.length === 0) {
    ElMessage.error("请至少添加一道题目");
    return;
  }
  schema.value.title = schema.value.title || form.templateName;
  form.schemaJson = stringifySchema(schema.value);
  if (form.templateType === "INTAKE") {
    form.stageId = undefined;
  }
  if (editingId.value) {
    await updateTemplate(editingId.value, form);
    ElMessage.success("模板更新成功");
  } else {
    await createTemplate(form);
    ElMessage.success("模板创建成功");
  }
  designerVisible.value = false;
  await loadData();
}

function getQuestionCount(schemaJson: string) {
  try {
    return parseSchemaJson(schemaJson, "问卷").items.length;
  } catch (error) {
    return 0;
  }
}

function canExportQr(row: Template) {
  if (row.status !== "ACTIVE") {
    return false;
  }
  if (row.templateType === "FOLLOW_UP" && !row.stageId) {
    return false;
  }
  return true;
}

function qrDisabledReason(row: Template) {
  if (row.status === "DRAFT") {
    return "草稿状态不能生码，先把状态改为「启用 (ACTIVE)」再来。";
  }
  if (row.status === "DISABLED") {
    return "该模板已停用，请先恢复为「启用 (ACTIVE)」再生码。";
  }
  if (row.templateType === "FOLLOW_UP" && !row.stageId) {
    return "随访模板必须先绑定一个随访阶段才能生成共享二维码。";
  }
  return "只有「启用 (ACTIVE)」状态的模板才能生码。";
}

function validateBasicInfo() {
  if (!form.templateCode.trim() || !form.templateName.trim()) {
    ElMessage.error("请先填写模板编码和模板名称");
    return false;
  }
  if (form.templateType === "FOLLOW_UP" && !form.stageId) {
    ElMessage.error("随访问卷必须选择所属阶段");
    return false;
  }
  return true;
}

async function loadQrImage() {
  if (!currentQrCode.value) {
    return;
  }
  if (qrImageUrl.value) {
    URL.revokeObjectURL(qrImageUrl.value);
    qrImageUrl.value = "";
  }
  qrImageError.value = "";
  qrImageLoading.value = true;
  try {
    qrImageUrl.value = await fetchQrCodeImage(currentQrCode.value.id);
  } catch (error) {
    const msg = extractErrorMessage(error, "二维码图片加载失败，请稍后重试");
    qrImageError.value = msg;
    ElMessage.error(msg);
  } finally {
    qrImageLoading.value = false;
  }
}

function extractErrorMessage(error: unknown, fallback: string): string {
  if (!error) return fallback;
  // axios 错误包装：error.response.data.message
  const anyErr = error as { response?: { data?: { message?: string } | ArrayBuffer }; message?: string };
  const data = anyErr.response?.data;
  if (data instanceof ArrayBuffer) {
    try {
      const parsed = JSON.parse(new TextDecoder("utf-8").decode(data));
      if (parsed?.message) return parsed.message;
    } catch {
      // fall through
    }
  } else if (data && typeof data === "object" && "message" in data && data.message) {
    return data.message as string;
  }
  if (anyErr.message) return anyErr.message;
  return fallback;
}

function downloadQrImage() {
  if (!qrImageUrl.value || !currentQrCode.value) {
    return;
  }
  const link = document.createElement("a");
  link.href = qrImageUrl.value;
  link.download = `${currentQrCode.value.targetCode || "questionnaire"}-qrcode.png`;
  link.click();
}

type SurveyQuestionLike = SurveySchema["items"][number];

onMounted(loadData);
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

.qr-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qr-meta {
  text-align: center;
}

.qr-title {
  font-size: 18px;
  font-weight: 600;
}

.qr-subtitle {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.qr-image-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 320px;
  background: #f8f9fc;
  border-radius: 16px;
}

.qr-image {
  width: 280px;
  height: 280px;
  object-fit: contain;
}

.qr-image-error {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  padding: 24px;
  text-align: center;
  color: #f56c6c;
  max-width: 360px;
}

.qr-image-error-text {
  font-size: 14px;
  word-break: break-all;
}

.qr-image-error-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.qr-image-error-hint code {
  background: #eef2f9;
  color: #303133;
  padding: 0 4px;
  border-radius: 4px;
}

.qr-desc {
  margin-top: 4px;
  word-break: break-all;
}

.designer-shell {
  min-height: calc(100vh - 110px);
  background: linear-gradient(180deg, #f6f7fb 0%, #eef2f9 100%);
  margin: -12px -20px -20px;
  padding: 16px 20px 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.designer-title {
  font-size: 20px;
  font-weight: 600;
}

.designer-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.designer-header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.designer-body {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 360px;
  gap: 18px;
  margin-top: 10px;
  align-items: start;
}

.toolbox-side {
  position: sticky;
  top: 16px;
  align-self: start;
}

.toolbox-bar {
  z-index: 5;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(8px);
  border: 1px solid #e8ebf2;
  border-radius: 18px;
  padding: 14px 18px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.06);
}

.toolbox-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
}

.toolbox-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbox-group {
  min-width: 0;
}

.toolbox-group-title {
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
}

.toolbox-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.toolbox-button {
  width: 100%;
  min-width: 0;
  border-radius: 10px;
  margin: 0;
}

.canvas-wrap {
  width: 100%;
  max-width: 860px;
  min-width: 0;
  justify-self: center;
}

.survey-cover {
  background: #fff;
  border-radius: 22px;
  padding: 40px 48px 28px;
  box-shadow: 0 18px 40px rgba(31, 35, 41, 0.08);
  margin-bottom: 18px;
}

.cover-title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding-left: 0;
  padding-right: 0;
}

.cover-title-input :deep(.el-input__inner) {
  text-align: center;
  font-size: 34px;
  font-weight: 700;
}

.survey-cover-desc {
  margin-top: 18px;
}

.survey-cover-desc :deep(.el-textarea__inner) {
  min-height: 96px;
  border: none;
  box-shadow: none;
  text-align: center;
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
}

.empty-block {
  background: #fff;
  border-radius: 18px;
  padding: 36px 0;
  box-shadow: 0 18px 40px rgba(31, 35, 41, 0.08);
}

.dragging-ghost {
  opacity: 0.5;
}

.dragging-chosen {
  cursor: grabbing;
}

.designer-side {
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 16px;
  justify-self: end;
  width: 360px;
}

.preview-title {
  font-size: 20px;
  font-weight: 700;
}

.preview-description {
  margin: 8px 0 16px;
  color: #606266;
}

.preview-panel {
  background: #f8f9fc;
  border-radius: 14px;
  padding: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 1440px) {
  .designer-body {
    grid-template-columns: 1fr;
  }

  .toolbox-side {
    position: static;
  }

  .designer-side {
    order: 3;
    position: static;
    width: 100%;
  }

  .canvas-wrap {
    width: 100%;
    min-width: 0;
    max-width: 100%;
  }
}
</style>
