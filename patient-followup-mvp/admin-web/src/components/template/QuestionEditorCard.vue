<template>
  <el-card class="question-card" shadow="never">
    <template #header>
      <div class="question-header">
        <div class="question-header-main">
          <div class="drag-handle" title="长按拖动排序">⋮⋮</div>
          <div class="question-header-content">
            <div class="question-title-row">
              <span class="question-number">{{ displayIndex }}</span>
              <el-input
                v-model="question.title"
                class="question-title-input"
                placeholder="请输入题目标题"
              />
              <span v-if="question.required" class="required-mark">*</span>
            </div>
            <el-input
              v-model="question.description"
              class="question-desc-input"
              placeholder="请输入题目说明（选填）"
            />
          </div>
        </div>
        <div class="question-actions">
          <el-button link @click="$emit('move-up')">上移</el-button>
          <el-button link @click="$emit('move-down')">下移</el-button>
          <el-button link @click="$emit('duplicate')">复制</el-button>
          <el-button link type="danger" @click="$emit('remove')">删除</el-button>
        </div>
      </div>
    </template>

    <el-form label-width="96px">
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="必填">
            <el-switch v-model="question.required" />
          </el-form-item>
        </el-col>
        <el-col :span="8" v-if="supportsPlaceholder">
          <el-form-item label="占位提示">
            <el-input v-model="question.placeholder" placeholder="请输入提示文案" />
          </el-form-item>
        </el-col>
        <el-col :span="8" v-if="question.type === 'date'">
          <el-form-item label="日期类型">
            <el-select v-model="question.dateType">
              <el-option label="日期" value="date" />
              <el-option label="日期时间" value="datetime" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="supportsOptions">
        <div class="sub-title">选项设置</div>
        <div v-for="(option, optionIndex) in question.options" :key="option.id" class="option-row">
          <span class="option-icon" :class="{ square: question.type === 'multiple_choice' }"></span>
          <el-input v-model="option.label" placeholder="请输入选项内容" />
          <el-button @click="removeOption(optionIndex)" :disabled="(question.options?.length || 0) <= 1">删除</el-button>
        </div>
        <div class="inline-actions">
          <el-button @click="addOption">新增选项</el-button>
          <el-switch v-model="question.allowOther" />
          <span>允许其他项</span>
        </div>
        <el-form-item label="最多选择" v-if="question.type === 'multiple_choice'">
          <el-input-number v-model="question.maxSelection" :min="1" :max="99" />
        </el-form-item>
      </template>

      <template v-if="question.type === 'number'">
        <div class="sub-title">数字范围</div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="最小值">
              <el-input-number v-model="question.min" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大值">
              <el-input-number v-model="question.max" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="question.unit" placeholder="如 分、次、ml" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <template v-if="question.type === 'rating'">
        <div class="sub-title">评分设置</div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="最低分">
              <el-input-number v-model="question.min" :min="0" :max="question.max || 10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高分">
              <el-input-number v-model="question.max" :min="question.min || 1" :max="10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分单位">
              <el-input v-model="question.unit" placeholder="分" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <template v-if="supportsMatrix">
        <div class="sub-title">矩阵设置</div>
        <div class="matrix-editor">
          <div class="matrix-toolbar">
            <div class="matrix-toolbar-left">
              <span class="matrix-mode-label">题型表现：</span>
              <span v-if="question.type === 'matrix_single'" class="matrix-choice-icon"></span>
              <span v-else-if="question.type === 'matrix_multiple'" class="matrix-choice-icon square"></span>
              <span v-else class="matrix-text-chip">填空</span>
            </div>
            <div class="inline-actions no-margin">
              <el-button @click="addColumn">新增列</el-button>
              <el-button @click="addRow">新增行</el-button>
            </div>
          </div>

          <div class="matrix-table-editor">
            <div class="matrix-editor-row matrix-editor-head">
              <div class="matrix-editor-cell row-head-cell">项目</div>
              <div v-for="(column, columnIndex) in question.columns" :key="column.id" class="matrix-editor-cell">
                <el-input v-model="column.label" placeholder="表头" />
                <el-button
                  link
                  type="danger"
                  @click="removeColumn(columnIndex)"
                  :disabled="(question.columns?.length || 0) <= 1"
                >
                  删除列
                </el-button>
              </div>
            </div>

            <div v-for="(row, rowIndex) in question.rows" :key="row.id" class="matrix-editor-row">
              <div class="matrix-editor-cell row-head-cell">
                <el-input v-model="row.label" placeholder="行标题" />
                <el-button
                  link
                  type="danger"
                  @click="removeRow(rowIndex)"
                  :disabled="(question.rows?.length || 0) <= 1"
                >
                  删除行
                </el-button>
              </div>
              <div v-for="column in question.columns" :key="column.id" class="matrix-editor-cell matrix-body-cell">
                <span v-if="question.type === 'matrix_single'" class="matrix-choice-icon"></span>
                <span v-else-if="question.type === 'matrix_multiple'" class="matrix-choice-icon square"></span>
                <span v-else class="matrix-text-placeholder">填写内容</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { SurveyQuestion } from "../../types";
import { createDimension, createOption } from "../../utils/templateDesigner";

const props = defineProps<{
  question: SurveyQuestion;
  index: number;
}>();

defineEmits<{
  remove: [];
  duplicate: [];
  "move-up": [];
  "move-down": [];
}>();

const displayIndex = computed(() => `${String(props.index + 1).padStart(2, "0")}`);
const supportsOptions = computed(() => ["single_choice", "multiple_choice", "dropdown"].includes(props.question.type));
const supportsMatrix = computed(() => ["matrix_single", "matrix_multiple", "matrix_text"].includes(props.question.type));
const supportsPlaceholder = computed(() => ["short_text", "long_text", "number"].includes(props.question.type));

function addOption() {
  props.question.options = [...(props.question.options || []), createOption(`选项${(props.question.options?.length || 0) + 1}`)];
}

function removeOption(index: number) {
  props.question.options = (props.question.options || []).filter((_, itemIndex) => itemIndex !== index);
}

function addRow() {
  props.question.rows = [...(props.question.rows || []), createDimension(`行${(props.question.rows?.length || 0) + 1}`)];
}

function removeRow(index: number) {
  props.question.rows = (props.question.rows || []).filter((_, itemIndex) => itemIndex !== index);
}

function addColumn() {
  props.question.columns = [...(props.question.columns || []), createDimension(`列${(props.question.columns?.length || 0) + 1}`)];
}

function removeColumn(index: number) {
  props.question.columns = (props.question.columns || []).filter((_, itemIndex) => itemIndex !== index);
}
</script>

<style scoped>
.question-card {
  margin-bottom: 16px;
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(31, 35, 41, 0.08);
}

.question-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.question-header-main {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  flex: 1;
}

.drag-handle {
  width: 24px;
  min-width: 24px;
  cursor: grab;
  color: #c0c4cc;
  font-size: 20px;
  line-height: 1.2;
  user-select: none;
}

.question-header-content {
  flex: 1;
}

.question-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-size: 30px;
  font-weight: 700;
}

.question-title-input {
  flex: 1;
}

.question-title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding-left: 0;
  padding-right: 0;
}

.question-title-input :deep(.el-input__inner) {
  font-size: 24px;
  font-weight: 600;
}

.required-mark {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
}

.question-desc-input {
  margin-top: 10px;
}

.question-desc-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding-left: 0;
  padding-right: 0;
}

.question-desc-input :deep(.el-input__inner) {
  color: #b0b4bc;
  font-size: 14px;
}

.question-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.question-card :deep(.el-card__header) {
  padding: 24px 28px 8px;
  border-bottom: none;
}

.question-card :deep(.el-card__body) {
  padding: 8px 28px 28px;
}

.sub-title {
  margin: 8px 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.option-row {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  gap: 12px;
  margin-bottom: 10px;
  align-items: center;
}

.inline-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.inline-actions.no-margin {
  margin-bottom: 0;
}

.option-icon {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid #c0c4cc;
  display: inline-block;
  box-sizing: border-box;
}

.option-icon.square {
  border-radius: 3px;
}

.matrix-editor {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.matrix-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8f9fc;
  border-bottom: 1px solid #ebeef5;
}

.matrix-toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.matrix-mode-label {
  color: #606266;
  font-size: 13px;
}

.matrix-table-editor {
  overflow-x: auto;
}

.matrix-editor-row {
  display: grid;
  grid-template-columns: 220px repeat(auto-fit, minmax(140px, 1fr));
}

.matrix-editor-head {
  background: #f5f7fa;
}

.matrix-editor-cell {
  min-height: 84px;
  padding: 10px 12px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.row-head-cell {
  background: #fafafa;
}

.matrix-body-cell {
  align-items: center;
  justify-content: center;
}

.matrix-choice-icon {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid #c0c4cc;
  display: inline-block;
  box-sizing: border-box;
}

.matrix-choice-icon.square {
  border-radius: 3px;
}

.matrix-text-chip,
.matrix-text-placeholder {
  color: #909399;
  font-size: 13px;
}
</style>
