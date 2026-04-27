package com.hospital.followup.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.hospital.followup.domain.FollowupStage;
import com.hospital.followup.domain.Patient;
import com.hospital.followup.domain.QuestionnaireQrCode;
import com.hospital.followup.domain.QuestionnaireTask;
import com.hospital.followup.domain.QuestionnaireTemplate;
import com.hospital.followup.domain.enums.QrCodeStatus;
import com.hospital.followup.domain.enums.QrCodeType;
import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import com.hospital.followup.domain.enums.TemplateStatus;
import com.hospital.followup.domain.enums.TemplateType;
import com.hospital.followup.dto.admin.QrCodeCreateRequest;
import com.hospital.followup.dto.admin.QrCodeView;
import com.hospital.followup.dto.publicapi.FollowUpSharedSubmissionRequest;
import com.hospital.followup.dto.publicapi.FollowUpSharedSubmissionResult;
import com.hospital.followup.dto.publicapi.PublicQrCodeResolveView;
import com.hospital.followup.dto.publicapi.PublicTaskDetailView;
import com.hospital.followup.dto.publicapi.PublicTemplatePayload;
import com.hospital.followup.dto.publicapi.TaskSubmissionRequest;
import com.hospital.followup.repository.PatientRepository;
import com.hospital.followup.repository.QuestionnaireQrCodeRepository;
import com.hospital.followup.repository.QuestionnaireTaskRepository;
import com.hospital.followup.repository.QuestionnaireTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);

    private static final String TOKEN_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 模板级二维码用于「共享随访」时，可以补填答卷的任务状态白名单。COMPLETED / CANCELLED 不允许重复填。 */
    private static final EnumSet<QuestionnaireTaskStatus> OPEN_TASK_STATUSES =
        EnumSet.of(QuestionnaireTaskStatus.PENDING, QuestionnaireTaskStatus.IN_PROGRESS, QuestionnaireTaskStatus.OVERDUE);

    public static final String SUBMIT_MODE_INTAKE = "INTAKE_NEW_PATIENT";
    public static final String SUBMIT_MODE_FOLLOW_UP_SHARED = "FOLLOW_UP_SHARED";
    public static final String SUBMIT_MODE_FOLLOW_UP_TASK = "FOLLOW_UP_TASK";

    private final QuestionnaireQrCodeRepository qrCodeRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final QuestionnaireTaskRepository taskRepository;
    private final PatientRepository patientRepository;
    private final QuestionnaireService questionnaireService;
    private final WechatMiniappService wechatMiniappService;

    public QrCodeService(
        QuestionnaireQrCodeRepository qrCodeRepository,
        QuestionnaireTemplateRepository templateRepository,
        QuestionnaireTaskRepository taskRepository,
        PatientRepository patientRepository,
        QuestionnaireService questionnaireService,
        WechatMiniappService wechatMiniappService
    ) {
        this.qrCodeRepository = qrCodeRepository;
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.patientRepository = patientRepository;
        this.questionnaireService = questionnaireService;
        this.wechatMiniappService = wechatMiniappService;
    }

    @Transactional
    public QrCodeView createTemplateQrCode(Long templateId, QrCodeCreateRequest request) {
        QuestionnaireTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("问卷模板不存在"));
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new IllegalArgumentException(
                "只有【启用】状态的模板才能生成二维码（当前状态：" + template.getStatus() + "）。"
                + "请在模板编辑页将状态改为 ACTIVE 后再试。"
            );
        }
        if (template.getTemplateType() == TemplateType.FOLLOW_UP && template.getStage() == null) {
            throw new IllegalArgumentException(
                "随访模板必须先绑定一个随访阶段才能生成共享二维码（同一阶段的所有患者会用到这个码）。"
            );
        }

        QuestionnaireQrCode qrCode = new QuestionnaireQrCode();
        qrCode.setQrType(QrCodeType.TEMPLATE);
        qrCode.setToken(generateUniqueToken());
        qrCode.setTemplate(template);
        qrCode.setStatus(QrCodeStatus.ACTIVE);
        qrCode.setPagePath(wechatMiniappService.getPagePath());
        qrCode.setExpiresAt(LocalDateTime.now().plusDays(resolveExpireDays(request, 30)));
        qrCode.setScanCount(0);
        log.info(
            "[qrcode] 创建模板级二维码 templateId={} templateType={} stageId={}",
            template.getId(),
            template.getTemplateType(),
            template.getStage() == null ? null : template.getStage().getId()
        );
        return toView(qrCodeRepository.save(qrCode));
    }

    /**
     * @deprecated 已切换为「模板级共享二维码 + 患者自报姓名手机匹配」的工作流，不再生成单患者任务码。
     * 旧数据继续可用（resolveForMiniapp 仍处理 TASK 类型），但不再新建。
     */
    @Deprecated
    @Transactional
    public QrCodeView createTaskQrCode(String taskNo, QrCodeCreateRequest request) {
        QuestionnaireTask task = taskRepository.findByTaskNo(taskNo)
            .orElseThrow(() -> new EntityNotFoundException("问卷任务不存在"));

        QuestionnaireQrCode qrCode = new QuestionnaireQrCode();
        qrCode.setQrType(QrCodeType.TASK);
        qrCode.setToken(generateUniqueToken());
        qrCode.setTask(task);
        qrCode.setStatus(QrCodeStatus.ACTIVE);
        qrCode.setPagePath(wechatMiniappService.getPagePath());
        qrCode.setExpiresAt(LocalDateTime.now().plusDays(resolveExpireDays(request, 30)));
        qrCode.setScanCount(0);
        return toView(qrCodeRepository.save(qrCode));
    }

    @Transactional(readOnly = true)
    public QrCodeView getQrCode(Long id) {
        QuestionnaireQrCode qrCode = qrCodeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("二维码不存在"));
        return toView(qrCode);
    }

    @Transactional
    public PublicQrCodeResolveView resolveForMiniapp(String token) {
        log.info("[scan] 收到小程序扫码请求 token={}", token);
        QuestionnaireQrCode qrCode = qrCodeRepository.findByToken(token)
            .orElseThrow(() -> {
                log.warn("[scan] token={} 在数据库找不到对应二维码", token);
                return new EntityNotFoundException("二维码不存在或已失效");
            });

        refreshStatus(qrCode);
        if (qrCode.getStatus() != QrCodeStatus.ACTIVE) {
            log.warn("[scan] token={} 二维码状态={}，拒绝", token, qrCode.getStatus());
            throw new IllegalArgumentException("二维码已失效，请重新生成");
        }

        qrCode.setLastAccessedAt(LocalDateTime.now());
        qrCode.setScanCount(qrCode.getScanCount() == null ? 1 : qrCode.getScanCount() + 1);
        log.info(
            "[scan] token={} 类型={} 第 {} 次扫码命中",
            token,
            qrCode.getQrType(),
            qrCode.getScanCount()
        );

        if (qrCode.getQrType() == QrCodeType.TEMPLATE) {
            QuestionnaireTemplate template = qrCode.getTemplate();
            FollowupStage stage = template.getStage();
            String submitMode = template.getTemplateType() == TemplateType.INTAKE
                ? SUBMIT_MODE_INTAKE
                : SUBMIT_MODE_FOLLOW_UP_SHARED;
            return new PublicQrCodeResolveView(
                qrCode.getQrType().name(),
                qrCode.getToken(),
                true,
                submitMode,
                null,
                null,
                null,
                stage == null ? null : String.valueOf(stage.getId()),
                stage == null ? null : stage.getStageName(),
                null,
                null,
                template.getTemplateType().name(),
                qrCode.getExpiresAt(),
                new PublicTemplatePayload(template.getTemplateCode(), template.getTemplateName(), template.getSchemaJson())
            );
        }

        // 旧 TASK 二维码：保留只读兼容，便于已发出去的码继续可用
        PublicTaskDetailView taskDetail = questionnaireService.getTaskDetail(qrCode.getTask().getTaskNo());
        return new PublicQrCodeResolveView(
            qrCode.getQrType().name(),
            qrCode.getToken(),
            false,
            SUBMIT_MODE_FOLLOW_UP_TASK,
            taskDetail.taskNo(),
            taskDetail.patientId(),
            taskDetail.patientName(),
            null,
            taskDetail.stageName(),
            taskDetail.dueDate(),
            taskDetail.status(),
            "FOLLOW_UP",
            qrCode.getExpiresAt(),
            taskDetail.template()
        );
    }

    /**
     * 共享随访码提交：
     * 1. 用 token 反查二维码 → 模板 → 随访阶段
     * 2. 用「手机号 + 姓名」匹配到既有患者
     * 3. 找 ta 在该阶段未完成的最早一条任务
     * 4. 复用 questionnaireService.submitTask 写答案、关任务
     */
    @Transactional
    public FollowUpSharedSubmissionResult submitFollowUpShared(FollowUpSharedSubmissionRequest request) {
        QuestionnaireQrCode qrCode = qrCodeRepository.findByToken(request.token())
            .orElseThrow(() -> new EntityNotFoundException("二维码不存在或已失效"));
        refreshStatus(qrCode);
        if (qrCode.getStatus() != QrCodeStatus.ACTIVE) {
            throw new IllegalArgumentException("二维码已失效，请重新生成");
        }
        if (qrCode.getQrType() != QrCodeType.TEMPLATE) {
            throw new IllegalArgumentException("仅模板级共享码支持本接口");
        }
        QuestionnaireTemplate template = qrCode.getTemplate();
        if (template.getTemplateType() != TemplateType.FOLLOW_UP) {
            throw new IllegalArgumentException("非随访模板不能走共享提交接口；首诊请使用 /api/public/intake-submissions");
        }
        FollowupStage stage = template.getStage();
        if (stage == null) {
            throw new IllegalStateException("随访模板未绑定阶段，无法定位任务（请联系管理员）");
        }

        String name = safeTrim(request.name());
        String phone = safeTrim(request.phone());
        if (name.isEmpty() || phone.isEmpty()) {
            throw new IllegalArgumentException("请正确填写姓名和手机号");
        }

        List<Patient> matchedPatients = patientRepository.findByPhoneAndName(phone, name);
        if (matchedPatients.isEmpty()) {
            // 退而求其次：仅按手机号匹配，提示姓名对不上以便患者发现自己填错了
            List<Patient> phoneMatches = patientRepository.findByPhone(phone);
            if (phoneMatches.isEmpty()) {
                log.info(
                    "[shared-followup] 拒绝：手机号未在患者库中 phone={} stageId={} templateId={}",
                    phone, stage.getId(), template.getId()
                );
                throw new IllegalArgumentException("您不在本期随访名单中，请联系医院核对手机号");
            }
            log.info(
                "[shared-followup] 拒绝：手机号匹配但姓名不一致 phone={} 输入姓名={} 库内姓名={}",
                phone, name, phoneMatches.get(0).getName()
            );
            throw new IllegalArgumentException("姓名与手机号不匹配，请检查后重新填写");
        }
        Patient patient = matchedPatients.get(0);
        if (matchedPatients.size() > 1) {
            log.warn(
                "[shared-followup] 同一手机号匹配到多个患者，使用首条 phone={} count={} pickedPatientId={}",
                phone, matchedPatients.size(), patient.getPatientId()
            );
        }

        List<QuestionnaireTask> openTasks = taskRepository
            .findByPatient_IdAndStage_IdAndStatusInOrderByDueDateAsc(patient.getId(), stage.getId(), OPEN_TASK_STATUSES);
        if (openTasks.isEmpty()) {
            log.info(
                "[shared-followup] 拒绝：患者在该阶段没有未完成任务 patientId={} stageId={}",
                patient.getPatientId(), stage.getId()
            );
            throw new IllegalArgumentException(
                "未找到您在【" + stage.getStageName() + "】阶段的待填写任务（可能已完成或尚未生成）"
            );
        }
        QuestionnaireTask task = openTasks.get(0);

        log.info(
            "[shared-followup] 提交命中 taskNo={} patientId={} stageId={}",
            task.getTaskNo(), patient.getPatientId(), stage.getId()
        );
        questionnaireService.submitTask(task.getTaskNo(), new TaskSubmissionRequest(request.answers()));

        return new FollowUpSharedSubmissionResult(task.getTaskNo(), patient.getName(), stage.getStageName());
    }

    @Transactional
    public byte[] generateQrImage(Long id) {
        QuestionnaireQrCode qrCode = qrCodeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("二维码不存在"));
        refreshStatus(qrCode);
        if (qrCode.getStatus() != QrCodeStatus.ACTIVE) {
            throw new IllegalArgumentException("二维码已失效，请重新生成");
        }
        if (wechatMiniappService.isConfigured()) {
            log.info(
                "[qrcode] 调用微信官方 getwxacodeunlimit 接口，token={} type={}",
                qrCode.getToken(),
                qrCode.getQrType()
            );
            return wechatMiniappService.generateMiniProgramCode(qrCode.getToken());
        }
        log.info(
            "[qrcode] 未配置微信小程序 appId/appSecret，回退生成调试 URL 二维码 token={}",
            qrCode.getToken()
        );
        return generateDebugQrCode(buildDebugUrl(qrCode.getToken()));
    }

    private void refreshStatus(QuestionnaireQrCode qrCode) {
        if (qrCode.getStatus() == QrCodeStatus.ACTIVE && qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            qrCode.setStatus(QrCodeStatus.EXPIRED);
        }
    }

    private QrCodeView toView(QuestionnaireQrCode qrCode) {
        return new QrCodeView(
            qrCode.getId(),
            qrCode.getQrType().name(),
            resolveTargetCode(qrCode),
            resolveTargetName(qrCode),
            qrCode.getToken(),
            qrCode.getStatus().name(),
            qrCode.getPagePath(),
            wechatMiniappService.isConfigured() ? "WECHAT_MINI_PROGRAM" : "DEBUG_URL",
            wechatMiniappService.isConfigured(),
            buildDebugUrl(qrCode.getToken()),
            qrCode.getExpiresAt()
        );
    }

    private String resolveTargetCode(QuestionnaireQrCode qrCode) {
        if (qrCode.getQrType() == QrCodeType.TEMPLATE && qrCode.getTemplate() != null) {
            return qrCode.getTemplate().getTemplateCode();
        }
        if (qrCode.getQrType() == QrCodeType.TASK && qrCode.getTask() != null) {
            return qrCode.getTask().getTaskNo();
        }
        return qrCode.getToken();
    }

    private String resolveTargetName(QuestionnaireQrCode qrCode) {
        if (qrCode.getQrType() == QrCodeType.TEMPLATE && qrCode.getTemplate() != null) {
            QuestionnaireTemplate template = qrCode.getTemplate();
            if (template.getTemplateType() == TemplateType.FOLLOW_UP && template.getStage() != null) {
                return template.getTemplateName() + "（" + template.getStage().getStageName() + " 共享码）";
            }
            return template.getTemplateName();
        }
        if (qrCode.getQrType() == QrCodeType.TASK && qrCode.getTask() != null) {
            String patientName = qrCode.getTask().getPatient().getName();
            String stageName = qrCode.getTask().getStage().getStageName();
            if (patientName == null || patientName.isBlank()) {
                return stageName;
            }
            return stageName == null || stageName.isBlank() ? patientName : patientName + " / " + stageName;
        }
        return qrCode.getToken();
    }

    private int resolveExpireDays(QrCodeCreateRequest request, int defaultValue) {
        return request == null || request.expireDays() == null ? defaultValue : request.expireDays();
    }

    private String buildDebugUrl(String token) {
        return wechatMiniappService.getPublicApiBaseUrl() + "/api/public/qrcode/resolve?token=" + token;
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = randomToken(16);
        } while (qrCodeRepository.findByToken(token).isPresent());
        return token;
    }

    private String randomToken(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(TOKEN_ALPHABET.charAt(SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return builder.toString();
    }

    private byte[] generateDebugQrCode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 480, 480);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException error) {
            throw new IllegalStateException("生成二维码图片失败");
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
