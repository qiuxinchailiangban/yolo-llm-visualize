# 医院患者随访问卷管理系统 MVP 设计

## 1. 项目整体架构设计

- 后端采用 `Spring Boot + JPA + MySQL + Redis + RESTful API`，承担核心业务、任务生成、状态流转、管理端与小程序接口输出。
- 管理端采用 `Vue 3 + TypeScript + Element Plus`，面向医院运营或随访管理员，处理患者、模板、任务与首页待办。
- 患者端采用独立 `微信小程序` 项目，负责首诊登记与后续问卷填写，不与后台管理端混在同一工程。
- Redis 在 MVP 中先作为预留能力，用于后续验证码、提醒队列、任务幂等键、缓存热点模板。
- 后续 RPA、消息提醒、AI 文案生成、权限与统计分析都通过独立模块或适配层接入，不污染当前主流程。

## 2. 模块划分

- `patient`：患者档案、patient_id 生成、患者状态管理。
- `stage`：随访阶段配置，支持新增术前、术后 N 天等动态阶段。
- `template`：问卷模板管理，支持首诊问卷与随访问卷。
- `task`：问卷任务生成、任务状态流转、逾期判定。
- `response`：患者答卷提交与答案归档。
- `dashboard`：首页待办聚合，包括今日手术、今日应填未填、可提醒患者。
- `reminder`：提醒规则与提醒任务预留表，MVP 先落模型不做真实发送。
- `integration`：后续对接微信通知、RPA 消息、AI 文案与权限体系的扩展接口。

## 3. 数据库表设计

- `patient`：患者主表，内部唯一标识使用 `patient_id`，姓名仅展示。
- `followup_stage`：随访阶段表，支持可配置阶段与偏移天数。
- `questionnaire_template`：问卷模板表，支持首诊和随访模板。
- `questionnaire_task`：患者问卷任务表，记录每个阶段任务状态。
- `questionnaire_response`：问卷答卷表，保存填写结果。
- `reminder_task`：提醒任务表，预留后续消息提醒能力。

## 4. 核心业务流程

1. 患者扫码进入小程序，加载首诊问卷模板。
2. 患者提交首诊信息，系统创建 `patient` 记录并生成唯一 `patient_id`。
3. 系统根据 `followup_stage` 配置和手术日期批量生成后续 `questionnaire_task`。
4. 管理端按患者查看所有任务，识别已完成、待填写、逾期任务。
5. 患者通过任务链接或二维码打开指定任务并提交问卷。
6. 系统写入 `questionnaire_response`，并将任务状态更新为已完成。
7. 管理首页聚合今日待办，并为后续提醒发送保留提醒任务入口。

## 5. 状态设计

### 患者状态

- `NEW`：刚建档，尚未进入稳定随访。
- `ACTIVE`：已建档，可继续维护。
- `FOLLOWING`：已生成并执行随访任务。
- `CLOSED`：随访结束。
- `ARCHIVED`：归档。

### 问卷任务状态

- `PENDING`：待填写。
- `IN_PROGRESS`：已打开但未最终提交，MVP 预留。
- `COMPLETED`：已完成。
- `OVERDUE`：逾期未填。
- `CANCELLED`：取消。

### 提醒任务状态

- `PENDING`：待调度。
- `READY`：满足发送条件。
- `SENT`：已发送。
- `FAILED`：发送失败。
- `CANCELLED`：取消。

## 6. API 清单

### 管理端

- `GET /api/admin/dashboard/todos`：首页待办。
- `GET /api/admin/patients`：患者列表。
- `POST /api/admin/patients`：新增患者。
- `GET /api/admin/patients/{patientId}`：患者详情及任务。
- `GET /api/admin/templates`：模板列表。
- `POST /api/admin/templates`：创建模板。
- `PUT /api/admin/templates/{id}`：更新模板。
- `GET /api/admin/stages`：阶段列表。

### 患者端

- `GET /api/public/intake-template`：获取首诊模板。
- `POST /api/public/intake-submissions`：提交首诊信息。
- `GET /api/public/tasks/{taskNo}`：获取某个任务问卷。
- `POST /api/public/tasks/{taskNo}/submit`：提交任务问卷。

## 7. 前端页面清单

### 管理后台

- 首页待办页
- 患者列表页
- 患者详情抽屉
- 问卷模板管理页
- 模板新增/编辑弹窗

### 微信小程序

- 首诊登记问卷页
- 随访问卷填写页

## 8. 第一版开发优先级

1. 建立后端骨架、数据库表与核心实体。
2. 打通首诊建档 -> 自动生成任务 -> 后续填写 -> 管理端查看闭环。
3. 完成患者管理、模板管理、首页待办与小程序问卷页。
4. 补充阶段管理、提醒调度、权限、统计、RPA 接入。
