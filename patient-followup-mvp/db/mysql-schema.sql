CREATE DATABASE IF NOT EXISTS patient_followup DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE patient_followup;

CREATE TABLE IF NOT EXISTS patient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(16),
  phone VARCHAR(32),
  birth_date DATE,
  surgery_date DATE,
  diagnosis VARCHAR(255),
  source_channel VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS followup_stage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stage_code VARCHAR(64) NOT NULL UNIQUE,
  stage_name VARCHAR(64) NOT NULL,
  day_offset INT NOT NULL,
  sort_order INT NOT NULL,
  enabled TINYINT(1) NOT NULL,
  reminder_enabled TINYINT(1) NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS questionnaire_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL UNIQUE,
  template_name VARCHAR(128) NOT NULL,
  template_type VARCHAR(32) NOT NULL,
  version VARCHAR(32) NOT NULL,
  stage_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  schema_json TEXT NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_template_stage FOREIGN KEY (stage_id) REFERENCES followup_stage(id)
);

CREATE TABLE IF NOT EXISTS questionnaire_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(32) NOT NULL UNIQUE,
  patient_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  due_date DATE NOT NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_task_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_task_stage FOREIGN KEY (stage_id) REFERENCES followup_stage(id),
  CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES questionnaire_template(id)
);

CREATE TABLE IF NOT EXISTS questionnaire_response (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  task_id BIGINT NULL,
  template_id BIGINT NOT NULL,
  answers_json LONGTEXT NOT NULL,
  submit_channel VARCHAR(32),
  submitted_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_response_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_response_task FOREIGN KEY (task_id) REFERENCES questionnaire_task(id),
  CONSTRAINT fk_response_template FOREIGN KEY (template_id) REFERENCES questionnaire_template(id)
);

CREATE TABLE IF NOT EXISTS reminder_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  questionnaire_task_id BIGINT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  reminder_channel VARCHAR(32) NOT NULL,
  target_conversation VARCHAR(128),
  content_preview VARCHAR(255),
  planned_at DATETIME NOT NULL,
  started_at DATETIME NULL,
  sent_at DATETIME NULL,
  finished_at DATETIME NULL,
  status VARCHAR(32) NOT NULL,
  fail_reason VARCHAR(255),
  execution_log TEXT,
  command_line VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_reminder_task FOREIGN KEY (questionnaire_task_id) REFERENCES questionnaire_task(id)
);

CREATE TABLE IF NOT EXISTS automation_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_no VARCHAR(32) NOT NULL UNIQUE,
  job_type VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  payload_json LONGTEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  planned_at DATETIME NOT NULL,
  claimed_at DATETIME NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  worker_id VARCHAR(64),
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(255),
  execution_log TEXT,
  result_json TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS questionnaire_qrcode (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qr_type VARCHAR(32) NOT NULL,
  token VARCHAR(32) NOT NULL UNIQUE,
  template_id BIGINT NULL,
  task_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  page_path VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  last_accessed_at DATETIME NULL,
  scan_count INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_qrcode_template FOREIGN KEY (template_id) REFERENCES questionnaire_template(id),
  CONSTRAINT fk_qrcode_task FOREIGN KEY (task_id) REFERENCES questionnaire_task(id)
);

INSERT INTO followup_stage (id, stage_code, stage_name, day_offset, sort_order, enabled, reminder_enabled, description, created_at, updated_at)
VALUES
  (1, 'PRE_OP', '术前', -1, 10, 1, 1, '手术前一天问卷', NOW(), NOW()),
  (2, 'POST_OP_DAY_1', '术后1天', 1, 20, 1, 1, '术后第1天问卷', NOW(), NOW()),
  (3, 'POST_OP_DAY_7', '术后7天', 7, 30, 1, 1, '术后第7天问卷', NOW(), NOW()),
  (4, 'POST_OP_DAY_30', '术后30天', 30, 40, 1, 1, '术后第30天问卷', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
