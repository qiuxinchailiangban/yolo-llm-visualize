package com.hospital.followup.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.hospital.followup.config.RpaIntegrationProperties;
import com.hospital.followup.config.WechatMiniappProperties;
import com.hospital.followup.domain.QuestionnaireTask;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 负责为「一键提醒 / 手动提醒」生成一张可以当成二维码图片直接在微信里发出去的 PNG。
 *
 * <p>真二维码链路（小程序 getwxacodeunlimit）目前还没完全跑通，这里先渲染一张
 * 带有占位文字的 QR PNG，保证整个"提醒发文字 + 发图片"的 RPA 流程能端到端验证。
 * 等真二维码通了，只要把 buildPayloadUrl() 换成真正的扫码落地 URL 即可。
 */
@Service
public class ReminderQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(ReminderQrCodeService.class);
    private static final int QR_SIZE = 480;
    private static final int CAPTION_HEIGHT = 56;

    private final RpaIntegrationProperties rpaProperties;
    private final WechatMiniappProperties miniappProperties;

    public ReminderQrCodeService(
        RpaIntegrationProperties rpaProperties,
        WechatMiniappProperties miniappProperties
    ) {
        this.rpaProperties = rpaProperties;
        this.miniappProperties = miniappProperties;
    }

    /**
     * 确保 {@code data/reminder-qr/{taskNo}.png} 存在并返回【绝对路径】。
     *
     * <p>目前写的是"假二维码"——二维码内容是一个形如
     * {@code https://<publicApiBaseUrl>/fake-followup/{taskNo}} 的占位 URL，扫了也不
     * 会跳到小程序。图片下方额外印一行文字提示这是占位。真二维码上线后只要替换
     * 二维码内容即可，文件名、路径、调用方都不用改。</p>
     *
     * @return 可直接写到 payload 里给 worker 读的绝对路径字符串；生成失败时返回 null
     */
    public String ensurePlaceholderQr(QuestionnaireTask task) {
        if (task == null || task.getTaskNo() == null || task.getTaskNo().isBlank()) {
            return null;
        }
        try {
            Path cacheDir = resolveCacheDir();
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve(task.getTaskNo() + ".png");

            // 已经生成过就复用——同一个 taskNo 再次触发提醒时不用重新画
            if (Files.exists(target) && Files.size(target) > 0) {
                return target.toAbsolutePath().toString();
            }

            BufferedImage image = renderPlaceholderQr(task);
            ImageIO.write(image, "PNG", target.toFile());
            log.info("[reminder-qr] 已生成占位二维码: {}", target.toAbsolutePath());
            return target.toAbsolutePath().toString();
        } catch (IOException | WriterException error) {
            log.warn("[reminder-qr] 生成占位二维码失败 taskNo={}: {}", task.getTaskNo(), error.getMessage());
            return null;
        }
    }

    private Path resolveCacheDir() {
        String configured = rpaProperties.getReminderQrCacheDir();
        if (configured == null || configured.isBlank()) {
            configured = "data/reminder-qr";
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private BufferedImage renderPlaceholderQr(QuestionnaireTask task) throws WriterException {
        String payload = buildPayloadUrl(task);
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
        BufferedImage qr = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig());

        BufferedImage canvas = new BufferedImage(QR_SIZE, QR_SIZE + CAPTION_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.drawImage(qr, 0, 0, null);

            g.setColor(new Color(0xE6, 0xA2, 0x3C));
            g.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
            String line1 = "【占位二维码 · 仅供流程联调】";
            g.drawString(line1, centerX(g, line1), QR_SIZE + 24);

            g.setColor(new Color(0x60, 0x60, 0x60));
            g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            String stage = task.getStage() == null ? "" : safe(task.getStage().getStageName());
            String line2 = "任务号 " + task.getTaskNo() + (stage.isEmpty() ? "" : (" · " + stage));
            g.drawString(line2, centerX(g, line2), QR_SIZE + 46);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private int centerX(Graphics2D g, String text) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        return Math.max(0, (QR_SIZE - textWidth) / 2);
    }

    private String buildPayloadUrl(QuestionnaireTask task) {
        String base = miniappProperties.getPublicApiBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://example.com";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/fake-followup/" + task.getTaskNo();
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
