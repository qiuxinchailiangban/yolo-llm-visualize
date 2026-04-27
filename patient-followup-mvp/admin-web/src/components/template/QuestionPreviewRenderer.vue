<template>
  <div class="preview-card">
    <div class="preview-title">
      <span>{{ index + 1 }}. {{ question.title }}</span>
      <el-tag v-if="question.required" type="danger" size="small">必填</el-tag>
    </div>
    <div v-if="question.description" class="preview-description">{{ question.description }}</div>

    <div v-if="['single_choice', 'multiple_choice', 'dropdown'].includes(question.type)" class="preview-block">
      <div v-if="question.type === 'single_choice'">
        <div v-for="option in question.options" :key="option.id" class="preview-option manual-option">
          <span class="preview-option-icon"></span>
          <span>{{ option.label }}</span>
        </div>
      </div>

      <div v-else-if="question.type === 'multiple_choice'">
        <div v-for="option in question.options" :key="option.id" class="preview-option manual-option">
          <span class="preview-option-icon square"></span>
          <span>{{ option.label }}</span>
        </div>
      </div>

      <el-select v-else placeholder="请选择" style="width: 100%">
        <el-option v-for="option in question.options" :key="option.id" :label="option.label" :value="option.value" />
      </el-select>
    </div>

    <div v-else-if="question.type === 'short_text'" class="preview-block">
      <el-input :placeholder="question.placeholder || '请输入'" />
    </div>

    <div v-else-if="question.type === 'long_text'" class="preview-block">
      <el-input type="textarea" :rows="4" :placeholder="question.placeholder || '请输入'" />
    </div>

    <div v-else-if="question.type === 'number'" class="preview-block">
      <el-input :placeholder="question.placeholder || '请输入数字'" />
      <div class="preview-helper" v-if="question.min !== null || question.max !== null">
        范围：{{ question.min ?? "-" }} ~ {{ question.max ?? "-" }} {{ question.unit || "" }}
      </div>
    </div>

    <div v-else-if="question.type === 'date'" class="preview-block">
      <el-date-picker :type="question.dateType === 'datetime' ? 'datetime' : 'date'" placeholder="请选择" style="width: 100%" />
    </div>

    <div v-else-if="question.type === 'rating'" class="preview-block">
      <el-rate />
      <div class="preview-helper">评分范围：{{ question.min ?? 1 }} ~ {{ question.max ?? 5 }}{{ question.unit || "分" }}</div>
    </div>

    <div v-else-if="['matrix_single', 'matrix_multiple', 'matrix_text'].includes(question.type)" class="preview-block">
      <div class="matrix-table">
        <div class="matrix-row matrix-head">
          <div class="matrix-cell first-cell">项目</div>
          <div v-for="column in question.columns" :key="column.id" class="matrix-cell">{{ column.label }}</div>
        </div>
        <div v-for="row in question.rows" :key="row.id" class="matrix-row">
          <div class="matrix-cell first-cell">{{ row.label }}</div>
          <div v-for="column in question.columns" :key="column.id" class="matrix-cell">
            <span v-if="question.type === 'matrix_single'" class="preview-option-icon"></span>
            <span v-else-if="question.type === 'matrix_multiple'" class="preview-option-icon square"></span>
            <div v-else class="matrix-text-input-mock">请输入</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SurveyQuestion } from "../../types";

defineProps<{
  question: SurveyQuestion;
  index: number;
}>();
</script>

<style scoped>
.preview-card {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  margin-bottom: 12px;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.preview-description {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.preview-block {
  margin-top: 14px;
}

.preview-option {
  margin-bottom: 8px;
}

.manual-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 2px;
}

.preview-option-icon {
  width: 14px;
  height: 14px;
  border: 1px solid #c0c4cc;
  border-radius: 50%;
  display: inline-block;
  box-sizing: border-box;
}

.preview-option-icon.square {
  border-radius: 3px;
}

.preview-helper {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.matrix-table {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.matrix-row {
  display: grid;
  grid-template-columns: 160px repeat(auto-fit, minmax(100px, 1fr));
}

.matrix-head {
  background: #f5f7fa;
}

.matrix-cell {
  padding: 10px 12px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
}

.first-cell {
  justify-content: flex-start;
}

.matrix-text-input-mock {
  width: 100%;
  min-height: 34px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  color: #c0c4cc;
  box-sizing: border-box;
}
</style>
