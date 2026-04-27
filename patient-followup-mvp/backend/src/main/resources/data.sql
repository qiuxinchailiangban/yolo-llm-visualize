INSERT INTO followup_stage (id, stage_code, stage_name, day_offset, sort_order, enabled, reminder_enabled, description, created_at, updated_at)
VALUES
  (1, 'PRE_OP', '术前', -1, 10, TRUE, TRUE, '手术前一天问卷', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'POST_OP_DAY_1', '术后1天', 1, 20, TRUE, TRUE, '术后第1天问卷', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'POST_OP_DAY_7', '术后7天', 7, 30, TRUE, TRUE, '术后第7天问卷', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'POST_OP_DAY_30', '术后30天', 30, 40, TRUE, TRUE, '术后第30天问卷', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO questionnaire_template (id, template_code, template_name, template_type, version, stage_id, status, schema_json, description, created_at, updated_at)
VALUES
  (1, 'INTAKE_V1', '首诊登记问卷', 'INTAKE', 'v1', NULL, 'ACTIVE',
   '{"title":"首诊登记问卷","items":[{"key":"chiefComplaint","label":"主要诉求","type":"textarea","required":true},{"key":"allergyHistory","label":"过敏史","type":"textarea","required":false},{"key":"notes","label":"补充说明","type":"textarea","required":false}]}',
   '患者首次扫码填写', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'PRE_OP_V1', '术前问卷', 'FOLLOW_UP', 'v1', 1, 'ACTIVE',
   '{"title":"术前问卷","items":[{"key":"sleepQuality","label":"昨晚睡眠情况","type":"input","required":true},{"key":"fasting","label":"是否按要求禁食","type":"radio","options":["是","否"],"required":true}]}',
   '术前阶段模板', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'POST_OP_DAY1_V1', '术后1天问卷', 'FOLLOW_UP', 'v1', 2, 'ACTIVE',
   '{"title":"术后1天问卷","items":[{"key":"painScore","label":"疼痛评分(0-10)","type":"number","required":true},{"key":"fever","label":"是否发热","type":"radio","options":["是","否"],"required":true}]}',
   '术后1天模板', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'POST_OP_DAY7_V1', '术后7天问卷', 'FOLLOW_UP', 'v1', 3, 'ACTIVE',
   '{"title":"术后7天问卷","items":[{"key":"woundRecovery","label":"伤口恢复情况","type":"textarea","required":true},{"key":"medicationCompliance","label":"是否按医嘱用药","type":"radio","options":["是","否"],"required":true}]}',
   '术后7天模板', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'POST_OP_DAY30_V1', '术后30天问卷', 'FOLLOW_UP', 'v1', 4, 'ACTIVE',
   '{"title":"术后30天问卷","items":[{"key":"dailyActivity","label":"日常活动恢复情况","type":"textarea","required":true},{"key":"revisitNeeded","label":"是否需要复诊","type":"radio","options":["是","否"],"required":true}]}',
   '术后30天模板', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
