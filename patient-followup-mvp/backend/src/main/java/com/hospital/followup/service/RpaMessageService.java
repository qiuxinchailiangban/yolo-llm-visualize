package com.hospital.followup.service;

import com.hospital.followup.config.RpaIntegrationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RpaMessageService {

    private final RpaIntegrationProperties properties;

    public RpaMessageService(RpaIntegrationProperties properties) {
        this.properties = properties;
    }

    public SendExecutionResult sendTextMessage(String targetConversation, String content, Integer countdownSeconds) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("RPA 联动未启用，请先配置 app.rpa-integration.enabled=true");
        }
        if (!StringUtils.hasText(targetConversation)) {
            throw new IllegalArgumentException("目标会话不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("提醒内容不能为空");
        }

        Path sdkRoot = Path.of(properties.getSdkRoot()).toAbsolutePath().normalize();
        if (!Files.isDirectory(sdkRoot)) {
            throw new IllegalArgumentException("RPA SDK 目录不存在: " + sdkRoot);
        }

        int resolvedCountdownSeconds = resolveCountdownSeconds(countdownSeconds);
        boolean useDelayedSend = resolvedCountdownSeconds > 0;
        Path scriptPath = buildScriptPath(sdkRoot, useDelayedSend);
        if (!Files.exists(scriptPath)) {
            throw new IllegalArgumentException("发送脚本不存在: " + scriptPath);
        }

        Path configPath = resolveConfigPath(sdkRoot);
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("RPA 配置文件不存在: " + configPath);
        }

        List<String> command = new ArrayList<>();
        command.add(properties.getPythonCommand());
        command.add(scriptPath.toString());
        command.add("--target");
        command.add(targetConversation.trim());
        command.add("--content");
        command.add(content.trim());
        command.add("--config");
        command.add(configPath.toString());
        command.add("--send-mode");
        command.add(properties.getSendMode());
        command.add("--wait-seconds");
        command.add(String.valueOf(properties.getWaitSeconds()));
        if (useDelayedSend) {
            command.add("--countdown-seconds");
            command.add(String.valueOf(resolvedCountdownSeconds));
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(sdkRoot.toFile());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            boolean finished = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            String output;
            try (var reader = process.inputReader()) {
                output = reader.lines().reduce("", (left, right) -> left + (left.isEmpty() ? "" : System.lineSeparator()) + right);
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException(
                    "RPA 发送超时，超过 " + Duration.ofSeconds(properties.getTimeoutSeconds()).toSeconds() + " 秒未完成"
                );
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalArgumentException("RPA 发送失败: " + summarizeOutput(output));
            }
            return new SendExecutionResult(command, output, resolvedCountdownSeconds, useDelayedSend);
        } catch (IOException e) {
            throw new IllegalArgumentException("启动 RPA 发送脚本失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("RPA 发送被中断", e);
        }
    }

    public int resolveCountdownSeconds(Integer countdownSeconds) {
        if (countdownSeconds != null) {
            return Math.max(0, countdownSeconds);
        }
        return Math.max(0, properties.getDefaultCountdownSeconds());
    }

    private Path resolveConfigPath(Path sdkRoot) {
        if (StringUtils.hasText(properties.getConfigPath())) {
            return Path.of(properties.getConfigPath()).toAbsolutePath().normalize();
        }
        return sdkRoot.resolve("config.yaml").normalize();
    }

    private Path buildScriptPath(Path sdkRoot, boolean useDelayedSend) {
        String scriptName = useDelayedSend ? properties.getDelayedSendScript() : properties.getSendScript();
        return sdkRoot.resolve(scriptName).normalize();
    }

    private String summarizeOutput(String output) {
        if (!StringUtils.hasText(output)) {
            return "无输出日志";
        }
        return output.length() > 500 ? output.substring(0, 500) + "..." : output;
    }

    public record SendExecutionResult(List<String> command, String output, int countdownSeconds, boolean delayedSend) {
    }
}
