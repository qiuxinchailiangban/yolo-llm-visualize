export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface Patient {
  patientId: string;
  name: string;
  gender?: string;
  phone?: string;
  birthDate?: string;
  surgeryDate?: string;
  surgeryScheduleTag?: string;
  surgeryTimeText?: string;
  diagnosis?: string;
  sourceChannel?: string;
  wechatChatroomUsername?: string;
  wechatChatroomDisplayName?: string;
  wechatGroupName?: string;
  status: string;
  createdAt: string;
}

export interface PatientForm {
  name: string;
  gender?: string;
  phone?: string;
  birthDate?: string;
  surgeryDate?: string;
  surgeryScheduleTag?: string;
  surgeryTimeText?: string;
  diagnosis?: string;
  sourceChannel?: string;
}

export interface PatientTask {
  taskNo: string;
  stageCode: string;
  stageName: string;
  templateName: string;
  status: string;
  dueDate: string;
  finishedAt?: string;
}

export type MessageTriggerRuleMode = "AUTO" | "MANUAL";
export type MessageTriggerTaskCategory = "PROCESS" | "REPLY" | "OTHER";
export type MessageTriggerType = "BIND_GROUP_IMMEDIATE" | "SURGERY_RELATIVE_DAY" | "KEYWORD_MESSAGE";
export type MessageTriggerTargetType = "BOUND_CHATROOM" | "PATIENT_NAME" | "CUSTOM_CONVERSATION";
export type MessageTriggerContentBlockType = "TEXT" | "IMAGE";
export type MessageTriggerConditionRelation = "ALL" | "ANY";
export type MessageTriggerFeedbackRule = "NONE" | "ANY_MESSAGE" | "KEYWORD" | "MANUAL_CONFIRM";
export type MessageTriggerConditionType =
  | "HAS_BOUND_CHATROOM"
  | "HAS_SURGERY_DATE"
  | "PATIENT_STATUS_IS"
  | "PATIENT_NAME_CONTAINS"
  | "DIAGNOSIS_CONTAINS"
  | "MESSAGE_DIRECTION_IS"
  | "MESSAGE_CONTENT_CONTAINS";

export interface MessageTriggerRuleCondition {
  conditionType: MessageTriggerConditionType;
  conditionValue?: string;
  sortOrder: number;
}

export interface MessageTriggerRuleContentBlock {
  blockType: MessageTriggerContentBlockType;
  textContent?: string;
  mediaPath?: string;
  mediaName?: string;
  sortOrder: number;
}

export interface MessageTriggerRule {
  id?: number;
  ruleCode?: string;
  ruleName: string;
  ruleMode: MessageTriggerRuleMode;
  taskCategory: MessageTriggerTaskCategory;
  triggerType: MessageTriggerType;
  relativeDayOffset?: number;
  keywordText?: string;
  keywordMatchMode?: "ANY" | "ALL";
  conditionRelation?: MessageTriggerConditionRelation;
  conditions?: MessageTriggerRuleCondition[];
  targetType: MessageTriggerTargetType;
  customTargetConversation?: string;
  contentBlocks: MessageTriggerRuleContentBlock[];
  feedbackRequired?: boolean;
  feedbackRule?: MessageTriggerFeedbackRule;
  feedbackKeywordText?: string;
  feedbackTimeoutHours?: number;
  enabled: boolean;
  sortOrder: number;
  description?: string;
  lastTriggeredAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MessageTriggerRuleMediaUploadResult {
  mediaPath: string;
  mediaName: string;
}

export interface MessageTriggerManualCandidate {
  candidateKey: string;
  ruleId: number;
  ruleName: string;
  triggerType: MessageTriggerType;
  patientId: string;
  patientName: string;
  targetConversation?: string;
  contentPreview?: string;
  sourceMessageKey?: string;
  sourceMessagePreview?: string;
  detectedReason?: string;
}

export interface MessageTriggerManualExecuteItem {
  ruleId: number;
  patientId: string;
  candidateKey: string;
  sourceMessageKey?: string;
}

export interface MessageTriggerManualExecuteItemResult {
  candidateKey: string;
  ruleName: string;
  patientId: string;
  patientName: string;
  status: string;
  message: string;
}

export interface MessageTriggerManualExecuteResult {
  total: number;
  queued: number;
  skipped: number;
  items: MessageTriggerManualExecuteItemResult[];
}

export interface PatientDetail {
  patient: Patient;
  tasks: PatientTask[];
  recentChatMessages: PatientChatMessage[];
}

export interface PatientProcessStep {
  id: number;
  stepCode: string;
  stepName: string;
  sortOrder: number;
  stepType: string;
  triggerMode: string;
  relativeDayOffset?: number;
  relativeBase?: string;
  status: string;
  statusReason?: string;
  feedbackSummary?: string;
  feedbackRequired: boolean;
  plannedDate?: string;
  plannedAt?: string;
  triggeredAt?: string;
  completedAt?: string;
  linkedQuestionnaireTaskNo?: string;
  linkedQuestionnaireStatus?: string;
  linkedQuestionnaireDueDate?: string;
  linkedQuestionnaireFinishedAt?: string;
  linkedQuestionnaireResponseSubmittedAt?: string;
  linkedQuestionnaireResponsePreview?: string;
  linkedAutomationJobNo?: string;
  linkedAutomationJobStatus?: string;
  linkedAutomationJobLastError?: string;
  linkedAutomationJobExecutionLog?: string;
  linkedMessageRuleCode?: string;
  applicableSurgeryTags?: string;
  displayHint?: string;
}

export interface PatientProcessOverview {
  instanceNo: string;
  patientId: string;
  patientName: string;
  diagnosis?: string;
  surgeryDate?: string;
  templateName: string;
  status: string;
  currentStepCode?: string;
  currentStepName?: string;
  totalStepCount: number;
  completedStepCount: number;
  waitingFeedbackCount: number;
  warningStepCount: number;
  progressPercent: number;
  summaryText?: string;
  updatedAt: string;
}

export interface PatientProcessDetail {
  instanceNo: string;
  patient: Patient;
  templateCode: string;
  templateName: string;
  status: string;
  currentStepCode?: string;
  currentStepName?: string;
  totalStepCount: number;
  completedStepCount: number;
  waitingFeedbackCount: number;
  warningStepCount: number;
  progressPercent: number;
  summaryText?: string;
  startedAt?: string;
  finishedAt?: string;
  steps: PatientProcessStep[];
}

export interface PatientProcessDashboard {
  totalPatients: number;
  activeInstances: number;
  waitingFeedbackPatients: number;
  warningPatients: number;
  items: PatientProcessOverview[];
}

export interface PatientProcessTemplateStep {
  id?: number;
  stepCode?: string;
  stepName: string;
  sortOrder: number;
  stepType: "MESSAGE" | "QUESTIONNAIRE";
  triggerMode: "EVENT_BIND_GROUP" | "SURGERY_RELATIVE";
  relativeDayOffset?: number;
  relativeBase?: string;
  description?: string;
  messageRuleCode?: string;
  messageRuleName?: string;
  stageCode?: string;
  templateCode?: string;
  completionRule?: string;
  applicableSurgeryTags?: string;
  feedbackRequired?: boolean;
  enabled?: boolean;
}

export interface PatientProcessExceptionItem {
  patientId: string;
  patientName: string;
  templateName: string;
  instanceNo: string;
  stepCode: string;
  stepName: string;
  exceptionType: string;
  severity: string;
  status: string;
  reason?: string;
  surgeryDate?: string;
  surgeryScheduleTag?: string;
  plannedDate?: string;
  triggeredAt?: string;
  linkedQuestionnaireTaskNo?: string;
  linkedAutomationJobNo?: string;
  updatedAt?: string;
}

export interface PatientProcessExceptionCenter {
  total: number;
  warningCount: number;
  feedbackTimeoutCount: number;
  sendFailureCount: number;
  items: PatientProcessExceptionItem[];
}

export interface PatientProcessTemplate {
  id?: number;
  templateCode?: string;
  templateName: string;
  templateCategory?: string;
  description?: string;
  active: boolean;
  defaultTemplate?: boolean;
  builtIn?: boolean;
  stepCount?: number;
  updatedAt?: string;
  steps: PatientProcessTemplateStep[];
}

export interface PatientChatMessage {
  id: number;
  chatroomName?: string;
  senderDisplayName?: string;
  senderUsername?: string;
  direction: string;
  messageType: string;
  contentPreview?: string;
  content?: string;
  messageTime?: string;
}

export interface WechatGroupLead {
  chatroomUsername: string;
  chatroomDisplayName?: string;
  rawGroupName?: string;
  parseStatus: "PARSED" | "FAILED";
  parseMessage?: string;
  groupStage?: string;
  eventDateText?: string;
  eventDate?: string;
  assistantDoctorName?: string;
  patientName?: string;
  surgerySite?: string;
  surgeryType?: string;
  linkedPatientId?: string;
  linkedPatientName?: string;
  reporterWorkerId?: string;
  sourceChannel?: string;
  firstMessageSnippet?: string;
  lastMessageSnippet?: string;
  discoveredAt?: string;
  lastSeenAt?: string;
  updatedAt?: string;
}

export interface PatientImportRowResult {
  rowNumber: number;
  patientName: string;
  phone?: string;
  surgeryDate?: string;
  action: "CREATED" | "UPDATED" | "SKIPPED";
  patientId?: string;
  taskCount: number;
  message: string;
}

export interface PatientImportResult {
  totalRows: number;
  successRows: number;
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  totalTasksGenerated: number;
  rows: PatientImportRowResult[];
}

export interface PatientTaskRebuildResult {
  totalPatients: number;
  rebuiltPatients: number;
  skippedPatients: number;
  totalTasksAffected: number;
}

export interface Stage {
  id: number;
  stageCode: string;
  stageName: string;
  dayOffset: number;
  sortOrder: number;
  enabled: boolean;
  reminderEnabled: boolean;
  description?: string;
}

export interface StageForm {
  stageCode: string;
  stageName: string;
  dayOffset: number;
  sortOrder: number;
  enabled: boolean;
  reminderEnabled: boolean;
  description?: string;
}

export interface Template {
  id?: number;
  templateCode: string;
  templateName: string;
  templateType: "INTAKE" | "FOLLOW_UP";
  version: string;
  stageId?: number | null;
  stageName?: string | null;
  status: "DRAFT" | "ACTIVE" | "DISABLED";
  schemaJson: string;
  description?: string;
}

export type QuestionCategory = "choice" | "text" | "score" | "matrix";

export type QuestionType =
  | "single_choice"
  | "multiple_choice"
  | "dropdown"
  | "short_text"
  | "long_text"
  | "number"
  | "date"
  | "rating"
  | "matrix_single"
  | "matrix_multiple"
  | "matrix_text";

export interface SurveyOption {
  id: string;
  label: string;
  value: string;
}

export interface MatrixDimensionItem {
  id: string;
  label: string;
  value: string;
}

export interface SurveyQuestion {
  id: string;
  key: string;
  title: string;
  category: QuestionCategory;
  type: QuestionType;
  required: boolean;
  description?: string;
  placeholder?: string;
  options?: SurveyOption[];
  rows?: MatrixDimensionItem[];
  columns?: MatrixDimensionItem[];
  allowOther?: boolean;
  min?: number | null;
  max?: number | null;
  unit?: string;
  dateType?: "date" | "datetime";
  maxSelection?: number | null;
}

export interface SurveySchema {
  title: string;
  description?: string;
  items: SurveyQuestion[];
 }

export interface QuestionTypeDefinition {
  type: QuestionType;
  category: QuestionCategory;
  label: string;
  description: string;
}

export interface TaskItem {
  taskNo: string;
  patientId: string;
  patientName: string;
  preferredConversation?: string;
  phone?: string;
  surgeryDate?: string;
  stageCode: string;
  stageName: string;
  templateName: string;
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "OVERDUE" | "CANCELLED";
  dueDate: string;
  finishedAt?: string;
}

export interface TaskReminderSendResult {
  reminderTaskId: number;
  taskNo: string;
  patientId: string;
  patientName: string;
  targetConversation: string;
  reminderChannel: string;
  status: "QUEUED" | "SENT" | "FAILED" | "READY" | "PENDING" | "CANCELLED";
  message: string;
  countdownSeconds?: number;
  startedAt?: string;
  sentAt?: string;
  output?: string;
  finishedAt?: string;
  commandLine?: string;
}

export interface ReminderTaskLog {
  id: number;
  taskNo: string;
  targetConversation?: string;
  contentPreview?: string;
  reminderChannel: string;
  status: "SENT" | "FAILED" | "READY" | "PENDING" | "CANCELLED";
  failReason?: string;
  plannedAt: string;
  startedAt?: string;
  sentAt?: string;
  finishedAt?: string;
  commandLine?: string;
  executionLog?: string;
}

export interface AutomationJob {
  id: number;
  jobNo: string;
  jobType: string;
  bizType: string;
  bizId: number;
  channel: string;
  status: "QUEUED" | "RUNNING" | "SUCCESS" | "FAILED" | "CANCELLED";
  plannedAt: string;
  claimedAt?: string;
  startedAt?: string;
  finishedAt?: string;
  workerId?: string;
  retryCount: number;
  lastError?: string;
  executionLog?: string;
}

export interface DashboardTodoItem {
  patientId: string;
  patientName: string;
  taskNo?: string;
  stageName?: string;
  dueDate?: string;
  remark?: string;
}

export interface DashboardData {
  surgeryTodayCount: number;
  questionnaireDueTodayCount: number;
  remindableCount: number;
  surgeriesToday: DashboardTodoItem[];
  questionnaireDueToday: DashboardTodoItem[];
  remindablePatients: DashboardTodoItem[];
  allPatients: DashboardTodoItem[];
}

export interface AdminUser {
  username: string;
  displayName: string;
  role: string;
}

export interface LoginResult {
  token: string;
  expiresAt: string;
  user: AdminUser;
}

export interface QrCodeInfo {
  id: number;
  qrType: "TEMPLATE" | "TASK";
  targetCode: string;
  targetName: string;
  token: string;
  status: "ACTIVE" | "EXPIRED" | "DISABLED";
  pagePath: string;
  imageMode: "WECHAT_MINI_PROGRAM" | "DEBUG_URL";
  wechatConfigured: boolean;
  debugUrl: string;
  expiresAt: string;
}
