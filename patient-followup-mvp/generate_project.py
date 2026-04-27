from pathlib import Path


ROOT = Path(r"e:\wxb\360sd\patient-followup-mvp")


def write(rel_path: str, content: str) -> None:
    path = ROOT / rel_path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


backend_files = {
    "backend/pom.xml": """
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>
    <groupId>com.hospital</groupId>
    <artifactId>patient-followup-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>patient-followup-backend</name>
    <description>Hospital patient follow-up MVP backend</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
""",
    "backend/src/main/resources/application.yml": """
server:
  port: 8080

spring:
  application:
    name: patient-followup-backend
  datasource:
    url: jdbc:h2:mem:followup;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
  jackson:
    default-property-inclusion: non_null
  data:
    redis:
      host: localhost
      port: 6379

app:
  cors:
    allowed-origins:
      - http://localhost:5173
      - https://servicewechat.com
""",
    "backend/src/main/resources/application-mysql.example.yml": """
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/patient_followup?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
  data:
    redis:
      host: 127.0.0.1
      port: 6379
""",
    "backend/src/main/resources/data.sql": """
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
""",
    "backend/src/main/java/com/hospital/followup/FollowupApplication.java": """
package com.hospital.followup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FollowupApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowupApplication.class, args);
    }
}
""",
    "backend/src/main/java/com/hospital/followup/config/CorsProperties.java": """
package com.hospital.followup.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/config/WebConfig.java": """
package com.hospital.followup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
    }
}
""",
    "backend/src/main/java/com/hospital/followup/common/ApiResponse.java": """
package com.hospital.followup.common;

public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
""",
    "backend/src/main/java/com/hospital/followup/common/GlobalExceptionHandler.java": """
package com.hospital.followup.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("请求参数校验失败");
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(EntityNotFoundException ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/BaseEntity.java": """
package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/enums/PatientStatus.java": """
package com.hospital.followup.domain.enums;

public enum PatientStatus {
    NEW,
    ACTIVE,
    FOLLOWING,
    CLOSED,
    ARCHIVED
}
""",
    "backend/src/main/java/com/hospital/followup/domain/enums/TemplateStatus.java": """
package com.hospital.followup.domain.enums;

public enum TemplateStatus {
    DRAFT,
    ACTIVE,
    DISABLED
}
""",
    "backend/src/main/java/com/hospital/followup/domain/enums/TemplateType.java": """
package com.hospital.followup.domain.enums;

public enum TemplateType {
    INTAKE,
    FOLLOW_UP
}
""",
    "backend/src/main/java/com/hospital/followup/domain/enums/QuestionnaireTaskStatus.java": """
package com.hospital.followup.domain.enums;

public enum QuestionnaireTaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    CANCELLED
}
""",
    "backend/src/main/java/com/hospital/followup/domain/enums/ReminderTaskStatus.java": """
package com.hospital.followup.domain.enums;

public enum ReminderTaskStatus {
    PENDING,
    READY,
    SENT,
    FAILED,
    CANCELLED
}
""",
    "backend/src/main/java/com/hospital/followup/domain/Patient.java": """
package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.PatientStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String patientId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String gender;

    @Column(length = 32)
    private String phone;

    private LocalDate birthDate;

    private LocalDate surgeryDate;

    @Column(length = 255)
    private String diagnosis;

    @Column(length = 32)
    private String sourceChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PatientStatus status;

    public Long getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getSurgeryDate() {
        return surgeryDate;
    }

    public void setSurgeryDate(LocalDate surgeryDate) {
        this.surgeryDate = surgeryDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/FollowupStage.java": """
package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "followup_stage")
public class FollowupStage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String stageCode;

    @Column(nullable = false, length = 64)
    private String stageName;

    @Column(nullable = false)
    private Integer dayOffset;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Boolean reminderEnabled;

    @Column(length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getDayOffset() {
        return dayOffset;
    }

    public void setDayOffset(Integer dayOffset) {
        this.dayOffset = dayOffset;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/QuestionnaireTemplate.java": """
package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "questionnaire_template")
public class QuestionnaireTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String templateCode;

    @Column(nullable = false, length = 128)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TemplateType templateType;

    @Column(nullable = false, length = 32)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private FollowupStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TemplateStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String schemaJson;

    @Column(length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public FollowupStage getStage() {
        return stage;
    }

    public void setStage(FollowupStage stage) {
        this.stage = stage;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/QuestionnaireTask.java": """
package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_task")
public class QuestionnaireTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String taskNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private FollowupStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestionnaireTaskStatus status;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public FollowupStage getStage() {
        return stage;
    }

    public void setStage(FollowupStage stage) {
        this.stage = stage;
    }

    public QuestionnaireTemplate getTemplate() {
        return template;
    }

    public void setTemplate(QuestionnaireTemplate template) {
        this.template = template;
    }

    public QuestionnaireTaskStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionnaireTaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/QuestionnaireResponse.java": """
package com.hospital.followup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_response")
public class QuestionnaireResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private QuestionnaireTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(length = 32)
    private String submitChannel;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public QuestionnaireTask getTask() {
        return task;
    }

    public void setTask(QuestionnaireTask task) {
        this.task = task;
    }

    public QuestionnaireTemplate getTemplate() {
        return template;
    }

    public void setTemplate(QuestionnaireTemplate template) {
        this.template = template;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public String getSubmitChannel() {
        return submitChannel;
    }

    public void setSubmitChannel(String submitChannel) {
        this.submitChannel = submitChannel;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/domain/ReminderTask.java": """
package com.hospital.followup.domain;

import com.hospital.followup.domain.enums.ReminderTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_task")
public class ReminderTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_task_id", nullable = false)
    private QuestionnaireTask questionnaireTask;

    @Column(nullable = false, length = 64)
    private String ruleCode;

    @Column(nullable = false, length = 32)
    private String reminderChannel;

    @Column(nullable = false)
    private LocalDateTime plannedAt;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReminderTaskStatus status;

    @Column(length = 255)
    private String failReason;

    public Long getId() {
        return id;
    }

    public QuestionnaireTask getQuestionnaireTask() {
        return questionnaireTask;
    }

    public void setQuestionnaireTask(QuestionnaireTask questionnaireTask) {
        this.questionnaireTask = questionnaireTask;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getReminderChannel() {
        return reminderChannel;
    }

    public void setReminderChannel(String reminderChannel) {
        this.reminderChannel = reminderChannel;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public ReminderTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ReminderTaskStatus status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/repository/PatientRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.enums.PatientStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    List<Patient> findByNameContainingIgnoreCaseOrPatientIdContainingIgnoreCaseOrderByCreatedAtDesc(String name, String patientId);

    List<Patient> findByStatusOrderByCreatedAtDesc(PatientStatus status);

    List<Patient> findBySurgeryDateOrderByCreatedAtDesc(LocalDate surgeryDate);

    boolean existsByPatientId(String patientId);
}
""",
    "backend/src/main/java/com/hospital/followup/repository/FollowupStageRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.FollowupStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowupStageRepository extends JpaRepository<FollowupStage, Long> {

    List<FollowupStage> findByEnabledTrueOrderBySortOrderAsc();

    Optional<FollowupStage> findByStageCode(String stageCode);
}
""",
    "backend/src/main/java/com/hospital/followup/repository/QuestionnaireTemplateRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireTemplateRepository extends JpaRepository<QuestionnaireTemplate, Long> {

    List<QuestionnaireTemplate> findByStatusOrderByUpdatedAtDesc(TemplateStatus status);

    List<QuestionnaireTemplate> findByTemplateNameContainingIgnoreCaseOrTemplateCodeContainingIgnoreCaseOrderByUpdatedAtDesc(String name, String code);

    Optional<QuestionnaireTemplate> findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType templateType, TemplateStatus status);

    Optional<QuestionnaireTemplate> findFirstByStageIdAndStatusOrderByUpdatedAtDesc(Long stageId, TemplateStatus status);
}
""",
    "backend/src/main/java/com/hospital/followup/repository/QuestionnaireTaskRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireTaskRepository extends JpaRepository<QuestionnaireTask, Long> {

    Optional<QuestionnaireTask> findByTaskNo(String taskNo);

    List<QuestionnaireTask> findByPatientPatientIdOrderByDueDateAsc(String patientId);

    List<QuestionnaireTask> findByDueDateAndStatusInOrderByDueDateAsc(LocalDate dueDate, Collection<QuestionnaireTaskStatus> statuses);

    List<QuestionnaireTask> findByDueDateLessThanAndStatus(LocalDate dueDate, QuestionnaireTaskStatus status);

    List<QuestionnaireTask> findTop20ByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(LocalDate dueDate, Collection<QuestionnaireTaskStatus> statuses);
}
""",
    "backend/src/main/java/com/hospital/followup/repository/QuestionnaireResponseRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.QuestionnaireResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponse, Long> {

    List<QuestionnaireResponse> findByPatientPatientIdOrderBySubmittedAtDesc(String patientId);
}
""",
    "backend/src/main/java/com/hospital/followup/repository/ReminderTaskRepository.java": """
package com.hospital.followup.repository;

import com.hospital.followup.domain.ReminderTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderTaskRepository extends JpaRepository<ReminderTask, Long> {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/CreatePatientRequest.java": """
package com.hospital.followup.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePatientRequest(
    @NotBlank(message = "不能为空") String name,
    String gender,
    String phone,
    LocalDate birthDate,
    LocalDate surgeryDate,
    String diagnosis,
    String sourceChannel
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/PatientTaskView.java": """
package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientTaskView(
    String taskNo,
    String stageCode,
    String stageName,
    String templateName,
    String status,
    LocalDate dueDate,
    LocalDateTime finishedAt
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/PatientView.java": """
package com.hospital.followup.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientView(
    String patientId,
    String name,
    String gender,
    String phone,
    LocalDate birthDate,
    LocalDate surgeryDate,
    String diagnosis,
    String sourceChannel,
    String status,
    LocalDateTime createdAt
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/PatientDetailView.java": """
package com.hospital.followup.dto.admin;

import java.util.List;

public record PatientDetailView(
    PatientView patient,
    List<PatientTaskView> tasks
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/TemplateUpsertRequest.java": """
package com.hospital.followup.dto.admin;

import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateUpsertRequest(
    @NotBlank(message = "不能为空") String templateCode,
    @NotBlank(message = "不能为空") String templateName,
    @NotNull(message = "不能为空") TemplateType templateType,
    @NotBlank(message = "不能为空") String version,
    Long stageId,
    @NotNull(message = "不能为空") TemplateStatus status,
    @NotBlank(message = "不能为空") String schemaJson,
    String description
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/TemplateView.java": """
package com.hospital.followup.dto.admin;

public record TemplateView(
    Long id,
    String templateCode,
    String templateName,
    String templateType,
    String version,
    Long stageId,
    String stageName,
    String status,
    String schemaJson,
    String description
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/StageView.java": """
package com.hospital.followup.dto.admin;

public record StageView(
    Long id,
    String stageCode,
    String stageName,
    Integer dayOffset,
    Integer sortOrder,
    Boolean enabled,
    Boolean reminderEnabled,
    String description
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/DashboardTodoItem.java": """
package com.hospital.followup.dto.admin;

public record DashboardTodoItem(
    String patientId,
    String patientName,
    String taskNo,
    String stageName,
    String dueDate,
    String remark
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/admin/DashboardView.java": """
package com.hospital.followup.dto.admin;

import java.util.List;

public record DashboardView(
    long surgeryTodayCount,
    long questionnaireDueTodayCount,
    long remindableCount,
    List<DashboardTodoItem> surgeriesToday,
    List<DashboardTodoItem> questionnaireDueToday,
    List<DashboardTodoItem> remindablePatients
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/publicapi/IntakeSubmissionRequest.java": """
package com.hospital.followup.dto.publicapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public record IntakeSubmissionRequest(
    @NotBlank(message = "姓名不能为空") String name,
    String gender,
    String phone,
    LocalDate birthDate,
    @NotNull(message = "手术日期不能为空") LocalDate surgeryDate,
    String diagnosis,
    Map<String, Object> answers
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/publicapi/TaskSubmissionRequest.java": """
package com.hospital.followup.dto.publicapi;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TaskSubmissionRequest(
    @NotNull(message = "answers不能为空") Map<String, Object> answers
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/publicapi/PublicTemplatePayload.java": """
package com.hospital.followup.dto.publicapi;

public record PublicTemplatePayload(
    String templateCode,
    String templateName,
    String schemaJson
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/publicapi/PublicTaskDetailView.java": """
package com.hospital.followup.dto.publicapi;

public record PublicTaskDetailView(
    String taskNo,
    String patientId,
    String patientName,
    String stageName,
    String dueDate,
    String status,
    PublicTemplatePayload template
) {
}
""",
    "backend/src/main/java/com/hospital/followup/dto/publicapi/IntakeSubmissionResult.java": """
package com.hospital.followup.dto.publicapi;

import java.util.List;

public record IntakeSubmissionResult(
    String patientId,
    List<String> createdTaskNos,
    String message
) {
}
""",
    "backend/src/main/java/com/hospital/followup/service/PatientIdGenerator.java": """
package com.hospital.followup.service;

import com.hospital.followup.repository.PatientRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class PatientIdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final PatientRepository patientRepository;

    public PatientIdGenerator(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public String nextId() {
        String prefix = "PT" + LocalDate.now().format(FORMATTER);
        String candidate;
        do {
            candidate = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
        } while (patientRepository.existsByPatientId(candidate));
        return candidate;
    }
}
""",
    "backend/src/main/java/com/hospital/followup/service/TaskNumberGenerator.java": """
package com.hospital.followup.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class TaskNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String nextNo() {
        return "TK" + LocalDateTime.now().format(FORMATTER) + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
""",
    "backend/src/main/java/com/hospital/followup/service/PatientService.java": """
package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.PatientStatus;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import com.hospital.followup.dto.admin.CreatePatientRequest;
import com.hospital.followup.dto.admin.PatientDetailView;
import com.hospital.followup.dto.admin.PatientTaskView;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.dto.publicapi.IntakeSubmissionRequest;
import com.hospital.followup.dto.publicapi.IntakeSubmissionResult;
import com.hospital.followup.repository.FollowupStageRepository;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final QuestionnaireTaskRepository questionnaireTaskRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final FollowupStageRepository stageRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final PatientIdGenerator patientIdGenerator;
    private final TaskNumberGenerator taskNumberGenerator;
    private final ObjectMapper objectMapper;

    public PatientService(
        PatientRepository patientRepository,
        QuestionnaireTaskRepository questionnaireTaskRepository,
        QuestionnaireTemplateRepository templateRepository,
        FollowupStageRepository stageRepository,
        QuestionnaireResponseRepository responseRepository,
        PatientIdGenerator patientIdGenerator,
        TaskNumberGenerator taskNumberGenerator,
        ObjectMapper objectMapper
    ) {
        this.patientRepository = patientRepository;
        this.questionnaireTaskRepository = questionnaireTaskRepository;
        this.templateRepository = templateRepository;
        this.stageRepository = stageRepository;
        this.responseRepository = responseRepository;
        this.patientIdGenerator = patientIdGenerator;
        this.taskNumberGenerator = taskNumberGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PatientView> listPatients(String keyword, PatientStatus status) {
        List<Patient> patients;
        if (keyword != null && !keyword.isBlank()) {
            patients = patientRepository.findByNameContainingIgnoreCaseOrPatientIdContainingIgnoreCaseOrderByCreatedAtDesc(keyword, keyword);
        } else if (status != null) {
            patients = patientRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            patients = patientRepository.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
        }
        return patients.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public PatientDetailView getPatientDetail(String patientId) {
        Patient patient = patientRepository.findByPatientId(patientId)
            .orElseThrow(() -> new EntityNotFoundException("患者不存在"));
        List<PatientTaskView> tasks = questionnaireTaskRepository.findByPatientPatientIdOrderByDueDateAsc(patientId)
            .stream()
            .map(task -> new PatientTaskView(
                task.getTaskNo(),
                task.getStage().getStageCode(),
                task.getStage().getStageName(),
                task.getTemplate().getTemplateName(),
                task.getStatus().name(),
                task.getDueDate(),
                task.getFinishedAt()
            ))
            .toList();
        return new PatientDetailView(toView(patient), tasks);
    }

    @Transactional
    public PatientView createPatient(CreatePatientRequest request) {
        Patient patient = new Patient();
        patient.setPatientId(patientIdGenerator.nextId());
        patient.setName(request.name());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setBirthDate(request.birthDate());
        patient.setSurgeryDate(request.surgeryDate());
        patient.setDiagnosis(request.diagnosis());
        patient.setSourceChannel(request.sourceChannel() == null || request.sourceChannel().isBlank() ? "ADMIN" : request.sourceChannel());
        patient.setStatus(PatientStatus.ACTIVE);
        patientRepository.save(patient);
        createFollowupTasks(patient);
        return toView(patient);
    }

    @Transactional
    public IntakeSubmissionResult submitIntake(IntakeSubmissionRequest request) {
        Patient patient = new Patient();
        patient.setPatientId(patientIdGenerator.nextId());
        patient.setName(request.name());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setBirthDate(request.birthDate());
        patient.setSurgeryDate(request.surgeryDate());
        patient.setDiagnosis(request.diagnosis());
        patient.setSourceChannel("WECHAT_MINIAPP");
        patient.setStatus(PatientStatus.ACTIVE);
        patientRepository.save(patient);

        QuestionnaireTemplate intakeTemplate = templateRepository
            .findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType.INTAKE, TemplateStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("未配置启用中的首诊模板"));

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setPatient(patient);
        response.setTemplate(intakeTemplate);
        response.setTask(null);
        response.setAnswersJson(writeJson(request.answers() == null ? Map.of() : request.answers()));
        response.setSubmitChannel("WECHAT_MINIAPP");
        response.setSubmittedAt(LocalDateTime.now());
        responseRepository.save(response);

        List<String> createdTaskNos = createFollowupTasks(patient);
        return new IntakeSubmissionResult(patient.getPatientId(), createdTaskNos, "提交成功，已生成后续随访任务");
    }

    @Transactional
    public List<String> createFollowupTasks(Patient patient) {
        if (patient.getSurgeryDate() == null) {
            return List.of();
        }
        List<String> taskNos = new ArrayList<>();
        List<FollowupStage> stages = stageRepository.findByEnabledTrueOrderBySortOrderAsc();
        for (FollowupStage stage : stages) {
            QuestionnaireTemplate template = templateRepository
                .findFirstByStageIdAndStatusOrderByUpdatedAtDesc(stage.getId(), TemplateStatus.ACTIVE)
                .orElse(null);
            if (template == null) {
                continue;
            }
            QuestionnaireTask task = new QuestionnaireTask();
            task.setTaskNo(taskNumberGenerator.nextNo());
            task.setPatient(patient);
            task.setStage(stage);
            task.setTemplate(template);
            task.setStatus(QuestionnaireTaskStatus.PENDING);
            task.setDueDate(patient.getSurgeryDate().plusDays(stage.getDayOffset()));
            questionnaireTaskRepository.save(task);
            taskNos.add(task.getTaskNo());
        }
        patient.setStatus(taskNos.isEmpty() ? PatientStatus.ACTIVE : PatientStatus.FOLLOWING);
        return taskNos;
    }

    private PatientView toView(Patient patient) {
        return new PatientView(
            patient.getPatientId(),
            patient.getName(),
            patient.getGender(),
            patient.getPhone(),
            patient.getBirthDate(),
            patient.getSurgeryDate(),
            patient.getDiagnosis(),
            patient.getSourceChannel(),
            patient.getStatus().name(),
            patient.getCreatedAt()
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("问卷答案JSON格式错误");
        }
    }
}
""",
    "backend/src/main/java/com/hospital/followup/service/TemplateService.java": """
package com.hospital.followup.service;

import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.dto.admin.StageView;
import com.hospital.followup.dto.admin.TemplateUpsertRequest;
import com.hospital.followup.dto.admin.TemplateView;
import com.hospital.followup.repository.FollowupStageRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final QuestionnaireTemplateRepository templateRepository;
    private final FollowupStageRepository stageRepository;

    public TemplateService(QuestionnaireTemplateRepository templateRepository, FollowupStageRepository stageRepository) {
        this.templateRepository = templateRepository;
        this.stageRepository = stageRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> listTemplates(String keyword, TemplateStatus status) {
        List<QuestionnaireTemplate> templates;
        if (keyword != null && !keyword.isBlank()) {
            templates = templateRepository.findByTemplateNameContainingIgnoreCaseOrTemplateCodeContainingIgnoreCaseOrderByUpdatedAtDesc(keyword, keyword);
        } else if (status != null) {
            templates = templateRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            templates = templateRepository.findAll().stream().sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())).toList();
        }
        return templates.stream().map(this::toView).toList();
    }

    @Transactional
    public TemplateView createTemplate(TemplateUpsertRequest request) {
        QuestionnaireTemplate template = new QuestionnaireTemplate();
        fillTemplate(template, request);
        return toView(templateRepository.save(template));
    }

    @Transactional
    public TemplateView updateTemplate(Long id, TemplateUpsertRequest request) {
        QuestionnaireTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("问卷模板不存在"));
        fillTemplate(template, request);
        return toView(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<StageView> listStages() {
        return stageRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
            .map(stage -> new StageView(
                stage.getId(),
                stage.getStageCode(),
                stage.getStageName(),
                stage.getDayOffset(),
                stage.getSortOrder(),
                stage.getEnabled(),
                stage.getReminderEnabled(),
                stage.getDescription()
            ))
            .toList();
    }

    private void fillTemplate(QuestionnaireTemplate template, TemplateUpsertRequest request) {
        template.setTemplateCode(request.templateCode());
        template.setTemplateName(request.templateName());
        template.setTemplateType(request.templateType());
        template.setVersion(request.version());
        template.setStatus(request.status());
        template.setSchemaJson(request.schemaJson());
        template.setDescription(request.description());
        if (request.stageId() != null) {
            FollowupStage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new EntityNotFoundException("随访阶段不存在"));
            template.setStage(stage);
        } else {
            template.setStage(null);
        }
    }

    private TemplateView toView(QuestionnaireTemplate template) {
        return new TemplateView(
            template.getId(),
            template.getTemplateCode(),
            template.getTemplateName(),
            template.getTemplateType().name(),
            template.getVersion(),
            template.getStage() == null ? null : template.getStage().getId(),
            template.getStage() == null ? null : template.getStage().getStageName(),
            template.getStatus().name(),
            template.getSchemaJson(),
            template.getDescription()
        );
    }
}
""",
    "backend/src/main/java/com/hospital/followup/service/QuestionnaireService.java": """
package com.hospital.followup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.domain.QuestionnaireResponse;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import com.hospital.followup.dto.publicapi.PublicTaskDetailView;
import com.hospital.followup.dto.publicapi.PublicTemplatePayload;
import com.hospital.followup.dto.publicapi.TaskSubmissionRequest;
import com.hospital.followup.repository.QuestionnaireResponseRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionnaireService {

    private final QuestionnaireTaskRepository taskRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public QuestionnaireService(
        QuestionnaireTaskRepository taskRepository,
        QuestionnaireResponseRepository responseRepository,
        QuestionnaireTemplateRepository templateRepository,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.responseRepository = responseRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PublicTemplatePayload getIntakeTemplate() {
        QuestionnaireTemplate template = templateRepository
            .findFirstByTemplateTypeAndStatusOrderByUpdatedAtDesc(TemplateType.INTAKE, TemplateStatus.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("首诊模板不存在"));
        return new PublicTemplatePayload(template.getTemplateCode(), template.getTemplateName(), template.getSchemaJson());
    }

    @Transactional
    public void refreshOverdueTasks() {
        for (QuestionnaireTask task : taskRepository.findByDueDateLessThanAndStatus(LocalDate.now(), QuestionnaireTaskStatus.PENDING)) {
            task.setStatus(QuestionnaireTaskStatus.OVERDUE);
        }
    }

    @Transactional(readOnly = true)
    public PublicTaskDetailView getTaskDetail(String taskNo) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        return new PublicTaskDetailView(
            task.getTaskNo(),
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            task.getStage().getStageName(),
            task.getDueDate().toString(),
            task.getStatus().name(),
            new PublicTemplatePayload(
                task.getTemplate().getTemplateCode(),
                task.getTemplate().getTemplateName(),
                task.getTemplate().getSchemaJson()
            )
        );
    }

    @Transactional
    public void submitTask(String taskNo, TaskSubmissionRequest request) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));
        if (task.getStatus() == QuestionnaireTaskStatus.COMPLETED) {
            throw new IllegalArgumentException("该任务已填写完成");
        }
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setPatient(task.getPatient());
        response.setTask(task);
        response.setTemplate(task.getTemplate());
        response.setAnswersJson(writeJson(request.answers()));
        response.setSubmitChannel("WECHAT_MINIAPP");
        response.setSubmittedAt(LocalDateTime.now());
        responseRepository.save(response);

        task.setStatus(QuestionnaireTaskStatus.COMPLETED);
        task.setFinishedAt(LocalDateTime.now());
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("问卷答案JSON格式错误");
        }
    }
}
""",
    "backend/src/main/java/com/hospital/followup/service/DashboardService.java": """
package com.hospital.followup.service;

import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.dto.admin.DashboardTodoItem;
import com.hospital.followup.dto.admin.DashboardView;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;
    private final QuestionnaireTaskRepository taskRepository;
    private final QuestionnaireService questionnaireService;

    public DashboardService(
        PatientRepository patientRepository,
        QuestionnaireTaskRepository taskRepository,
        QuestionnaireService questionnaireService
    ) {
        this.patientRepository = patientRepository;
        this.taskRepository = taskRepository;
        this.questionnaireService = questionnaireService;
    }

    @Transactional
    public DashboardView getDashboard() {
        questionnaireService.refreshOverdueTasks();
        LocalDate today = LocalDate.now();

        List<Patient> surgeriesToday = patientRepository.findBySurgeryDateOrderByCreatedAtDesc(today);
        List<QuestionnaireTask> dueToday = taskRepository.findByDueDateAndStatusInOrderByDueDateAsc(
            today,
            Set.of(QuestionnaireTaskStatus.PENDING, QuestionnaireTaskStatus.OVERDUE)
        );
        List<QuestionnaireTask> remindable = taskRepository.findTop20ByDueDateLessThanEqualAndStatusInOrderByDueDateAsc(
            today,
            Set.of(QuestionnaireTaskStatus.PENDING, QuestionnaireTaskStatus.OVERDUE)
        );

        return new DashboardView(
            surgeriesToday.size(),
            dueToday.size(),
            remindable.size(),
            surgeriesToday.stream().map(this::mapPatientTodo).toList(),
            dueToday.stream().map(this::mapTaskTodo).toList(),
            remindable.stream().map(this::mapTaskTodo).toList()
        );
    }

    private DashboardTodoItem mapPatientTodo(Patient patient) {
        return new DashboardTodoItem(
            patient.getPatientId(),
            patient.getName(),
            null,
            null,
            patient.getSurgeryDate() == null ? null : patient.getSurgeryDate().toString(),
            patient.getDiagnosis()
        );
    }

    private DashboardTodoItem mapTaskTodo(QuestionnaireTask task) {
        return new DashboardTodoItem(
            task.getPatient().getPatientId(),
            task.getPatient().getName(),
            task.getTaskNo(),
            task.getStage().getStageName(),
            task.getDueDate().toString(),
            task.getStatus().name()
        );
    }
}
""",
    "backend/src/main/java/com/hospital/followup/controller/admin/AdminPatientController.java": """
package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.domain.enums.PatientStatus;
import com.hospital.followup.dto.admin.CreatePatientRequest;
import com.hospital.followup.dto.admin.PatientDetailView;
import com.hospital.followup.dto.admin.PatientView;
import com.hospital.followup.service.PatientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final PatientService patientService;

    public AdminPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ApiResponse<List<PatientView>> listPatients(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) PatientStatus status
    ) {
        return ApiResponse.ok(patientService.listPatients(keyword, status));
    }

    @GetMapping("/{patientId}")
    public ApiResponse<PatientDetailView> getPatient(@PathVariable String patientId) {
        return ApiResponse.ok(patientService.getPatientDetail(patientId));
    }

    @PostMapping
    public ApiResponse<PatientView> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.ok(patientService.createPatient(request), "患者创建成功");
    }
}
""",
    "backend/src/main/java/com/hospital/followup/controller/admin/AdminQuestionnaireTemplateController.java": """
package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.dto.admin.StageView;
import com.hospital.followup.dto.admin.TemplateUpsertRequest;
import com.hospital.followup.dto.admin.TemplateView;
import com.hospital.followup.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminQuestionnaireTemplateController {

    private final TemplateService templateService;

    public AdminQuestionnaireTemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/templates")
    public ApiResponse<List<TemplateView>> listTemplates(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) TemplateStatus status
    ) {
        return ApiResponse.ok(templateService.listTemplates(keyword, status));
    }

    @PostMapping("/templates")
    public ApiResponse<TemplateView> createTemplate(@Valid @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(templateService.createTemplate(request), "模板创建成功");
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<TemplateView> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(templateService.updateTemplate(id, request), "模板更新成功");
    }

    @GetMapping("/stages")
    public ApiResponse<List<StageView>> listStages() {
        return ApiResponse.ok(templateService.listStages());
    }
}
""",
    "backend/src/main/java/com/hospital/followup/controller/admin/AdminDashboardController.java": """
package com.hospital.followup.controller.admin;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.admin.DashboardView;
import com.hospital.followup.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/todos")
    public ApiResponse<DashboardView> getDashboardTodos() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
""",
    "backend/src/main/java/com/hospital/followup/controller/publicapi/PublicQuestionnaireController.java": """
package com.hospital.followup.controller.publicapi;

import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.dto.publicapi.IntakeSubmissionRequest;
import com.hospital.followup.dto.publicapi.IntakeSubmissionResult;
import com.hospital.followup.dto.publicapi.PublicTaskDetailView;
import com.hospital.followup.dto.publicapi.PublicTemplatePayload;
import com.hospital.followup.dto.publicapi.TaskSubmissionRequest;
import com.hospital.followup.service.PatientService;
import com.hospital.followup.service.QuestionnaireService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicQuestionnaireController {

    private final QuestionnaireService questionnaireService;
    private final PatientService patientService;

    public PublicQuestionnaireController(QuestionnaireService questionnaireService, PatientService patientService) {
        this.questionnaireService = questionnaireService;
        this.patientService = patientService;
    }

    @GetMapping("/intake-template")
    public ApiResponse<PublicTemplatePayload> getIntakeTemplate() {
        return ApiResponse.ok(questionnaireService.getIntakeTemplate());
    }

    @PostMapping("/intake-submissions")
    public ApiResponse<IntakeSubmissionResult> submitIntake(@Valid @RequestBody IntakeSubmissionRequest request) {
        return ApiResponse.ok(patientService.submitIntake(request), "首诊信息提交成功");
    }

    @GetMapping("/tasks/{taskNo}")
    public ApiResponse<PublicTaskDetailView> getTaskDetail(@PathVariable String taskNo) {
        questionnaireService.refreshOverdueTasks();
        return ApiResponse.ok(questionnaireService.getTaskDetail(taskNo));
    }

    @PostMapping("/tasks/{taskNo}/submit")
    public ApiResponse<Void> submitTask(@PathVariable String taskNo, @Valid @RequestBody TaskSubmissionRequest request) {
        questionnaireService.submitTask(taskNo, request);
        return ApiResponse.ok(null, "问卷提交成功");
    }
}
""",
    "backend/src/test/java/com/hospital/followup/FollowupApplicationTests.java": """
package com.hospital.followup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FollowupApplicationTests {

    @Test
    void contextLoads() {
    }
}
""",
}


admin_web_files = {
    "admin-web/package.json": """
{
  "name": "patient-followup-admin-web",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.8.4",
    "element-plus": "^2.9.8",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "@types/node": "^22.13.4",
    "@vitejs/plugin-vue": "^5.2.1",
    "typescript": "^5.7.3",
    "vite": "^6.2.0",
    "vue-tsc": "^2.2.8"
  }
}
""",
    "admin-web/index.html": """
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>患者随访管理后台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
""",
    "admin-web/tsconfig.json": """
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "moduleResolution": "Node",
    "strict": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "types": ["node"]
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"]
}
""",
    "admin-web/src/env.d.ts": """
declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}
""",
    "admin-web/vite.config.ts": """
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
  },
});
""",
    "admin-web/src/main.ts": """
import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";

createApp(App).use(router).use(ElementPlus).mount("#app");
""",
    "admin-web/src/router/index.ts": """
import { createRouter, createWebHistory } from "vue-router";
import DashboardView from "../views/DashboardView.vue";
import PatientListView from "../views/PatientListView.vue";
import TemplateListView from "../views/TemplateListView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/dashboard", component: DashboardView },
    { path: "/patients", component: PatientListView },
    { path: "/templates", component: TemplateListView },
  ],
});

export default router;
""",
    "admin-web/src/api/client.ts": """
import axios from "axios";

export const request = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 10000,
});

request.interceptors.response.use((response) => response.data);
""",
    "admin-web/src/types.ts": """
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
  diagnosis?: string;
  sourceChannel?: string;
  status: string;
  createdAt: string;
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

export interface PatientDetail {
  patient: Patient;
  tasks: PatientTask[];
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
}
""",
    "admin-web/src/api/patient.ts": """
import { request } from "./client";
import type { Patient, PatientDetail } from "../types";

export async function fetchPatients(keyword = ""): Promise<Patient[]> {
  const res = await request.get("/api/admin/patients", { params: { keyword } });
  return res.data;
}

export async function fetchPatientDetail(patientId: string): Promise<PatientDetail> {
  const res = await request.get(`/api/admin/patients/${patientId}`);
  return res.data;
}

export async function createPatient(payload: Record<string, unknown>): Promise<Patient> {
  const res = await request.post("/api/admin/patients", payload);
  return res.data;
}
""",
    "admin-web/src/api/template.ts": """
import { request } from "./client";
import type { Stage, Template } from "../types";

export async function fetchTemplates(): Promise<Template[]> {
  const res = await request.get("/api/admin/templates");
  return res.data;
}

export async function fetchStages(): Promise<Stage[]> {
  const res = await request.get("/api/admin/stages");
  return res.data;
}

export async function createTemplate(payload: Template): Promise<Template> {
  const res = await request.post("/api/admin/templates", payload);
  return res.data;
}

export async function updateTemplate(id: number, payload: Template): Promise<Template> {
  const res = await request.put(`/api/admin/templates/${id}`, payload);
  return res.data;
}
""",
    "admin-web/src/api/dashboard.ts": """
import { request } from "./client";
import type { DashboardData } from "../types";

export async function fetchDashboard(): Promise<DashboardData> {
  const res = await request.get("/api/admin/dashboard/todos");
  return res.data;
}
""",
    "admin-web/src/App.vue": """
<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">患者随访管理系统</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/dashboard">首页待办</el-menu-item>
        <el-menu-item index="/patients">患者管理</el-menu-item>
        <el-menu-item index="/templates">问卷模板</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">医院患者随访问卷管理系统 MVP</el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activeMenu = computed(() => route.path);
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.sidebar {
  background: #001529;
  color: #fff;
}

.logo {
  padding: 20px 16px;
  font-size: 18px;
  font-weight: 600;
}

.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  font-size: 18px;
  font-weight: 600;
}

.main {
  background: #f5f7fa;
}
</style>
""",
    "admin-web/src/views/DashboardView.vue": """
<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card><div class="metric"><span>今日手术患者</span><strong>{{ dashboard?.surgeryTodayCount ?? 0 }}</strong></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="metric"><span>今日待填问卷</span><strong>{{ dashboard?.questionnaireDueTodayCount ?? 0 }}</strong></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="metric"><span>可发送提醒</span><strong>{{ dashboard?.remindableCount ?? 0 }}</strong></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="8">
        <el-card header="今天要手术的患者">
          <el-table :data="dashboard?.surgeriesToday ?? []" size="small">
            <el-table-column prop="patientId" label="患者ID" />
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="dueDate" label="手术日" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="今天应填未填">
          <el-table :data="dashboard?.questionnaireDueToday ?? []" size="small">
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="stageName" label="阶段" />
            <el-table-column prop="dueDate" label="应填日" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="可发送提醒">
          <el-table :data="dashboard?.remindablePatients ?? []" size="small">
            <el-table-column prop="patientName" label="姓名" />
            <el-table-column prop="stageName" label="阶段" />
            <el-table-column prop="remark" label="状态" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { fetchDashboard } from "../api/dashboard";
import type { DashboardData } from "../types";

const dashboard = ref<DashboardData>();

async function loadData() {
  dashboard.value = await fetchDashboard();
}

onMounted(loadData);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}

.metric strong {
  font-size: 28px;
  color: #409eff;
}

.mt16 {
  margin-top: 16px;
}
</style>
""",
    "admin-web/src/views/PatientListView.vue": """
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" placeholder="按姓名或patient_id搜索" clearable @keyup.enter="loadPatients" />
            <el-button type="primary" @click="loadPatients">搜索</el-button>
          </div>
          <el-button type="success" @click="openCreate">新增患者</el-button>
        </div>
      </template>

      <el-table :data="patients" @row-click="showDetail">
        <el-table-column prop="patientId" label="Patient ID" width="170" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="90" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="surgeryDate" label="手术日期" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="sourceChannel" label="来源" width="140" />
        <el-table-column prop="diagnosis" label="诊断" min-width="200" />
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="患者详情" size="50%">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Patient ID">{{ detail.patient.patientId }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.patient.name }}</el-descriptions-item>
          <el-descriptions-item label="手术日期">{{ detail.patient.surgeryDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.patient.status }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.patient.phone }}</el-descriptions-item>
          <el-descriptions-item label="诊断">{{ detail.patient.diagnosis }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>问卷任务</el-divider>
        <el-table :data="detail.tasks">
          <el-table-column prop="taskNo" label="任务编号" width="200" />
          <el-table-column prop="stageName" label="阶段" width="120" />
          <el-table-column prop="templateName" label="模板" width="160" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="dueDate" label="应填日期" width="120" />
          <el-table-column prop="finishedAt" label="完成时间" min-width="180" />
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" title="新增患者" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别"><el-input v-model="form.gender" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="手术日期"><el-date-picker v-model="form.surgeryDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="form.diagnosis" type="textarea" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="form.sourceChannel" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { onMounted, reactive, ref } from "vue";
import { createPatient, fetchPatientDetail, fetchPatients } from "../api/patient";
import type { Patient, PatientDetail } from "../types";

const keyword = ref("");
const patients = ref<Patient[]>([]);
const detail = ref<PatientDetail>();
const detailVisible = ref(false);
const createVisible = ref(false);
const form = reactive({
  name: "",
  gender: "",
  phone: "",
  birthDate: "",
  surgeryDate: "",
  diagnosis: "",
  sourceChannel: "ADMIN",
});

async function loadPatients() {
  patients.value = await fetchPatients(keyword.value);
}

async function showDetail(row: Patient) {
  detail.value = await fetchPatientDetail(row.patientId);
  detailVisible.value = true;
}

function openCreate() {
  Object.assign(form, {
    name: "",
    gender: "",
    phone: "",
    birthDate: "",
    surgeryDate: "",
    diagnosis: "",
    sourceChannel: "ADMIN",
  });
  createVisible.value = true;
}

async function submitCreate() {
  await createPatient(form);
  ElMessage.success("患者创建成功");
  createVisible.value = false;
  await loadPatients();
}

onMounted(loadPatients);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  width: 420px;
}
</style>
""",
    "admin-web/src/views/TemplateListView.vue": """
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span>问卷模板管理</span>
          <el-button type="primary" @click="openCreate">新增模板</el-button>
        </div>
      </template>

      <el-table :data="templates">
        <el-table-column prop="templateCode" label="模板编码" width="150" />
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="templateType" label="类型" width="120" />
        <el-table-column prop="stageName" label="阶段" width="140" />
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新增模板'" width="760px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板编码" required><el-input v-model="form.templateCode" /></el-form-item>
        <el-form-item label="模板名称" required><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="模板类型" required>
          <el-select v-model="form.templateType">
            <el-option label="首诊问卷" value="INTAKE" />
            <el-option label="随访问卷" value="FOLLOW_UP" />
          </el-select>
        </el-form-item>
        <el-form-item label="随访阶段">
          <el-select v-model="form.stageId" clearable>
            <el-option v-for="item in stages" :key="item.id" :label="item.stageName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本" required><el-input v-model="form.version" /></el-form-item>
        <el-form-item label="状态" required>
          <el-select v-model="form.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="Schema JSON" required>
          <el-input v-model="form.schemaJson" type="textarea" :rows="10" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { onMounted, reactive, ref } from "vue";
import { createTemplate, fetchStages, fetchTemplates, updateTemplate } from "../api/template";
import type { Stage, Template } from "../types";

const templates = ref<Template[]>([]);
const stages = ref<Stage[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number>();
const form = reactive<Template>({
  templateCode: "",
  templateName: "",
  templateType: "FOLLOW_UP",
  version: "v1",
  stageId: undefined,
  status: "DRAFT",
  schemaJson: '{\"title\":\"新问卷\",\"items\":[]}',
  description: "",
});

async function loadData() {
  templates.value = await fetchTemplates();
  stages.value = await fetchStages();
}

function openCreate() {
  editingId.value = undefined;
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
  dialogVisible.value = true;
}

function openEdit(row: Template) {
  editingId.value = row.id;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submitForm() {
  if (editingId.value) {
    await updateTemplate(editingId.value, form);
    ElMessage.success("模板更新成功");
  } else {
    await createTemplate(form);
    ElMessage.success("模板创建成功");
  }
  dialogVisible.value = false;
  await loadData();
}

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
}
</style>
""",
}


miniapp_files = {
    "miniapp/app.js": """
App({});
""",
    "miniapp/app.json": """
{
  "pages": [
    "pages/questionnaire/index"
  ],
  "window": {
    "navigationBarTitleText": "问卷填写",
    "navigationBarBackgroundColor": "#1677ff",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f5f7fa"
  }
}
""",
    "miniapp/app.wxss": """
page {
  background: #f5f7fa;
  color: #303133;
}
""",
    "miniapp/project.config.json": """
{
  "miniprogramRoot": "./",
  "projectname": "patient-followup-miniapp",
  "description": "患者随访问卷小程序 MVP"
}
""",
    "miniapp/utils/request.js": """
const BASE_URL = "http://localhost:8080";

function request({ url, method = "GET", data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        "content-type": "application/json",
      },
      success(res) {
        if (res.data && res.data.success) {
          resolve(res.data.data);
        } else {
          reject(new Error(res.data?.message || "请求失败"));
        }
      },
      fail: reject,
    });
  });
}

module.exports = { request };
""",
    "miniapp/pages/questionnaire/index.json": """
{
  "navigationBarTitleText": "患者问卷"
}
""",
    "miniapp/pages/questionnaire/index.js": """
const { request } = require("../../utils/request");

Page({
  data: {
    mode: "intake",
    loading: true,
    taskNo: "",
    patientId: "",
    title: "",
    stageName: "",
    items: [],
    patientForm: {
      name: "",
      gender: "",
      phone: "",
      birthDate: "",
      surgeryDate: "",
      diagnosis: "",
    },
    answers: {},
  },

  onLoad(options) {
    const taskNo = options.taskNo || "";
    if (taskNo) {
      this.setData({ mode: "task", taskNo });
      this.loadTask(taskNo);
    } else {
      this.loadIntakeTemplate();
    }
  },

  async loadIntakeTemplate() {
    try {
      const data = await request({ url: "/api/public/intake-template" });
      const schema = JSON.parse(data.schemaJson || "{\"items\":[]}");
      this.setData({
        title: data.templateName,
        items: schema.items || [],
        loading: false,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
      this.setData({ loading: false });
    }
  },

  async loadTask(taskNo) {
    try {
      const data = await request({ url: `/api/public/tasks/${taskNo}` });
      const schema = JSON.parse(data.template.schemaJson || "{\"items\":[]}");
      this.setData({
        title: data.template.templateName,
        patientId: data.patientId,
        stageName: data.stageName,
        items: schema.items || [],
        loading: false,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
      this.setData({ loading: false });
    }
  },

  onPatientInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`patientForm.${field}`]: e.detail.value,
    });
  },

  onAnswerInput(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({
      [`answers.${key}`]: e.detail.value,
    });
  },

  async submit() {
    try {
      if (this.data.mode === "task") {
        await request({
          url: `/api/public/tasks/${this.data.taskNo}/submit`,
          method: "POST",
          data: { answers: this.data.answers },
        });
      } else {
        await request({
          url: "/api/public/intake-submissions",
          method: "POST",
          data: {
            ...this.data.patientForm,
            answers: this.data.answers,
          },
        });
      }
      wx.showToast({ title: "提交成功", icon: "success" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },
});
""",
    "miniapp/pages/questionnaire/index.wxml": """
<view class="page">
  <view class="card" wx:if="{{loading}}">加载中...</view>

  <view wx:else>
    <view class="card">
      <view class="title">{{title}}</view>
      <view class="subtitle" wx:if="{{mode === 'task'}}">患者ID：{{patientId}} / 阶段：{{stageName}}</view>
    </view>

    <view class="card" wx:if="{{mode === 'intake'}}">
      <view class="section-title">患者基本信息</view>
      <input class="input" placeholder="姓名" data-field="name" bindinput="onPatientInput" />
      <input class="input" placeholder="性别" data-field="gender" bindinput="onPatientInput" />
      <input class="input" placeholder="手机号" data-field="phone" bindinput="onPatientInput" />
      <input class="input" placeholder="出生日期，如 1990-01-01" data-field="birthDate" bindinput="onPatientInput" />
      <input class="input" placeholder="手术日期，如 2026-04-22" data-field="surgeryDate" bindinput="onPatientInput" />
      <textarea class="textarea" placeholder="诊断信息" data-field="diagnosis" bindinput="onPatientInput" />
    </view>

    <view class="card">
      <view class="section-title">问卷内容</view>
      <block wx:for="{{items}}" wx:key="key">
        <view class="field">
          <view class="label">{{item.label}}</view>
          <textarea wx:if="{{item.type === 'textarea'}}" class="textarea" data-key="{{item.key}}" bindinput="onAnswerInput" />
          <input wx:elif="{{item.type === 'number'}}" class="input" type="number" data-key="{{item.key}}" bindinput="onAnswerInput" />
          <input wx:else class="input" data-key="{{item.key}}" bindinput="onAnswerInput" />
        </view>
      </block>
    </view>

    <button class="submit" type="primary" bindtap="submit">提交问卷</button>
  </view>
</view>
""",
    "miniapp/pages/questionnaire/index.wxss": """
.page {
  padding: 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
}

.subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #909399;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.field {
  margin-bottom: 20rpx;
}

.label {
  margin-bottom: 10rpx;
  font-size: 28rpx;
}

.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 20rpx;
}

.textarea {
  min-height: 140rpx;
}

.submit {
  margin-top: 24rpx;
}
""",
}


shared_files = {
    "db/mysql-schema.sql": """
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
  planned_at DATETIME NOT NULL,
  sent_at DATETIME NULL,
  status VARCHAR(32) NOT NULL,
  fail_reason VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_reminder_task FOREIGN KEY (questionnaire_task_id) REFERENCES questionnaire_task(id)
);

INSERT INTO followup_stage (id, stage_code, stage_name, day_offset, sort_order, enabled, reminder_enabled, description, created_at, updated_at)
VALUES
  (1, 'PRE_OP', '术前', -1, 10, 1, 1, '手术前一天问卷', NOW(), NOW()),
  (2, 'POST_OP_DAY_1', '术后1天', 1, 20, 1, 1, '术后第1天问卷', NOW(), NOW()),
  (3, 'POST_OP_DAY_7', '术后7天', 7, 30, 1, 1, '术后第7天问卷', NOW(), NOW()),
  (4, 'POST_OP_DAY_30', '术后30天', 30, 40, 1, 1, '术后第30天问卷', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
""",
    "docs/architecture.md": """
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
""",
    "README.md": """
# patient-followup-mvp

医院患者随访问卷管理系统 MVP，包含：

- `backend`：Spring Boot 后端
- `admin-web`：Vue 3 管理后台
- `miniapp`：微信小程序 MVP
- `db/mysql-schema.sql`：MySQL 建表脚本
- `docs/architecture.md`：架构设计说明

## 快速启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认使用内存 H2 启动，便于快速演示；切换到 MySQL 时可参考 `application-mysql.example.yml`。

### 管理端

```bash
cd admin-web
npm install
npm run dev
```

### 小程序

- 用微信开发者工具打开 `miniapp`
- 默认接口地址为 `http://localhost:8080`
- 真机调试时请改成可访问的后端地址
""",
}


for group in (backend_files, admin_web_files, miniapp_files, shared_files):
    for path, content in group.items():
        write(path, content)

print("Project files generated.")
