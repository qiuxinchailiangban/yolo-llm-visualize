import type {
  MatrixDimensionItem,
  QuestionType,
  QuestionTypeDefinition,
  SurveyOption,
  SurveyQuestion,
  SurveySchema,
} from "../types";

const questionTypeDefinitions: QuestionTypeDefinition[] = [
  { type: "single_choice", category: "choice", label: "单选题", description: "适合单项选择" },
  { type: "multiple_choice", category: "choice", label: "多选题", description: "适合多项选择" },
  { type: "dropdown", category: "choice", label: "下拉题", description: "适合选项较多场景" },
  { type: "short_text", category: "text", label: "单行填空", description: "简短文本输入" },
  { type: "long_text", category: "text", label: "多行填空", description: "较长文本输入" },
  { type: "number", category: "text", label: "数字题", description: "数值填写，可配范围" },
  { type: "date", category: "text", label: "日期题", description: "日期或时间填写" },
  { type: "rating", category: "score", label: "评分题", description: "星级或分值评价" },
  { type: "matrix_single", category: "matrix", label: "矩阵单选", description: "每行单选一项" },
  { type: "matrix_multiple", category: "matrix", label: "矩阵多选", description: "每行可多选" },
  { type: "matrix_text", category: "matrix", label: "矩阵填空", description: "按行列填写内容" },
];

export const questionCategories = [
  { key: "choice", label: "选择题" },
  { key: "text", label: "填空题" },
  { key: "score", label: "评分题" },
  { key: "matrix", label: "矩阵题" },
] as const;

export function getQuestionTypeDefinitions() {
  return questionTypeDefinitions;
}

export function createEmptySchema(title = "新问卷"): SurveySchema {
  return {
    title,
    description: "",
    items: [],
  };
}

export function createQuestionByType(type: QuestionType, index: number): SurveyQuestion {
  const definition = questionTypeDefinitions.find((item) => item.type === type);
  const question: SurveyQuestion = {
    id: generateId("q"),
    key: `q_${index + 1}`,
    title: `${definition?.label || "题目"}${index + 1}`,
    category: definition?.category || "text",
    type,
    required: false,
    description: "",
    placeholder: "",
    allowOther: false,
    min: null,
    max: null,
    maxSelection: null,
    unit: "",
    dateType: "date",
    options: [],
    rows: [],
    columns: [],
  };

  if (["single_choice", "multiple_choice", "dropdown"].includes(type)) {
    question.options = [createOption("选项1"), createOption("选项2")];
  }

  if (type === "rating") {
    question.min = 1;
    question.max = 5;
    question.unit = "分";
  }

  if (["matrix_single", "matrix_multiple"].includes(type)) {
    question.rows = [createDimension("行1"), createDimension("行2")];
    question.columns = [createDimension("列1"), createDimension("列2"), createDimension("列3")];
  }

  if (type === "matrix_text") {
    question.rows = [createDimension("项目1"), createDimension("项目2")];
    question.columns = [createDimension("填写内容")];
  }

  if (type === "number") {
    question.placeholder = "请输入数字";
  }

  if (type === "date") {
    question.dateType = "date";
  }

  if (type === "short_text") {
    question.placeholder = "请输入";
  }

  if (type === "long_text") {
    question.placeholder = "请输入详细内容";
  }

  return question;
}

export function createOption(label = "新选项"): SurveyOption {
  return {
    id: generateId("opt"),
    label,
    value: generateKey(label),
  };
}

export function createDimension(label = "新项"): MatrixDimensionItem {
  return {
    id: generateId("dim"),
    label,
    value: generateKey(label),
  };
}

export function duplicateQuestion(question: SurveyQuestion, index: number): SurveyQuestion {
  const clone = JSON.parse(JSON.stringify(question)) as SurveyQuestion;
  clone.id = generateId("q");
  clone.key = `${question.key || "q"}_${index + 1}`;
  clone.title = `${question.title}（副本）`;
  clone.options = clone.options?.map((option) => ({ ...option, id: generateId("opt") }));
  clone.rows = clone.rows?.map((item) => ({ ...item, id: generateId("dim") }));
  clone.columns = clone.columns?.map((item) => ({ ...item, id: generateId("dim") }));
  return clone;
}

export function parseSchemaJson(schemaJson: string, fallbackTitle: string): SurveySchema {
  if (!schemaJson || !schemaJson.trim()) {
    return createEmptySchema(fallbackTitle);
  }

  const raw = JSON.parse(schemaJson) as {
    title?: string;
    description?: string;
    items?: unknown[];
  };

  return {
    title: raw.title || fallbackTitle,
    description: raw.description || "",
    items: Array.isArray(raw.items) ? raw.items.map((item, index) => normalizeQuestion(item, index)) : [],
  };
}

export function stringifySchema(schema: SurveySchema) {
  const normalized = {
    ...schema,
    items: schema.items.map((question, index) => ({
      ...question,
      key: question.key?.trim() || `q_${index + 1}`,
      options: question.options?.map((option, optionIndex) => ({
        ...option,
        value: option.value?.trim() || generateKey(option.label || `选项${optionIndex + 1}`),
      })),
      rows: question.rows?.map((row, rowIndex) => ({
        ...row,
        value: row.value?.trim() || generateKey(row.label || `行${rowIndex + 1}`),
      })),
      columns: question.columns?.map((column, columnIndex) => ({
        ...column,
        value: column.value?.trim() || generateKey(column.label || `列${columnIndex + 1}`),
      })),
    })),
  };
  return JSON.stringify(normalized, null, 2);
}

function normalizeQuestion(item: unknown, index: number): SurveyQuestion {
  const raw = (item || {}) as Record<string, unknown>;
  const mappedType = mapLegacyType(String(raw.type || "short_text"));
  const question = createQuestionByType(mappedType, index);

  question.id = String(raw.id || generateId("q"));
  question.key = String(raw.key || `q_${index + 1}`);
  question.title = String(raw.title || raw.label || question.title);
  question.required = Boolean(raw.required);
  question.description = String(raw.description || "");
  question.placeholder = String(raw.placeholder || "");
  question.allowOther = Boolean(raw.allowOther);
  question.min = toNullableNumber(raw.min, question.min);
  question.max = toNullableNumber(raw.max, question.max);
  question.unit = String(raw.unit || question.unit || "");
  question.dateType = raw.dateType === "datetime" ? "datetime" : "date";
  question.maxSelection = toNullableNumber(raw.maxSelection, null);

  if (Array.isArray(raw.options)) {
    question.options = raw.options.map((option, optIndex) => normalizeOption(option, optIndex));
  }

  if (Array.isArray(raw.rows)) {
    question.rows = raw.rows.map((row, rowIndex) => normalizeDimension(row, rowIndex));
  }

  if (Array.isArray(raw.columns)) {
    question.columns = raw.columns.map((column, columnIndex) => normalizeDimension(column, columnIndex));
  }

  return question;
}

function normalizeOption(option: unknown, index: number): SurveyOption {
  const raw = (option || {}) as Record<string, unknown>;
  const label = String(raw.label || raw.value || `选项${index + 1}`);
  return {
    id: String(raw.id || generateId("opt")),
    label,
    value: String(raw.value || generateKey(label)),
  };
}

function normalizeDimension(item: unknown, index: number): MatrixDimensionItem {
  const raw = (item || {}) as Record<string, unknown>;
  const label = String(raw.label || raw.value || `项目${index + 1}`);
  return {
    id: String(raw.id || generateId("dim")),
    label,
    value: String(raw.value || generateKey(label)),
  };
}

function mapLegacyType(rawType: string): QuestionType {
  const normalized = rawType.toLowerCase();
  switch (normalized) {
    case "radio":
    case "single_choice":
      return "single_choice";
    case "checkbox":
    case "multiple_choice":
      return "multiple_choice";
    case "select":
    case "dropdown":
      return "dropdown";
    case "textarea":
    case "long_text":
      return "long_text";
    case "number":
      return "number";
    case "date":
      return "date";
    case "rating":
      return "rating";
    case "matrix_single":
      return "matrix_single";
    case "matrix_multiple":
      return "matrix_multiple";
    case "matrix_text":
      return "matrix_text";
    case "input":
    case "short_text":
    default:
      return "short_text";
  }
}

function generateId(prefix: string) {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
}

function generateKey(label: string) {
  return label
    .replace(/\s+/g, "_")
    .replace(/[^\w\u4e00-\u9fa5]/g, "")
    .toLowerCase() || generateId("key");
}

function toNullableNumber(value: unknown, fallback: number | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return fallback ?? null;
  }
  const num = Number(value);
  return Number.isNaN(num) ? fallback ?? null : num;
}
