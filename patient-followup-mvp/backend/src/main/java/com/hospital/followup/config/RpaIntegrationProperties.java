package com.hospital.followup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rpa-integration")
public class RpaIntegrationProperties {

    private boolean enabled = false;
    private String sdkRoot = "E:/wxb/360sd/omni_bot_sdk-1.0.6";
    private String configPath;
    private String pythonCommand = "python";
    private String sendScript = "send_once.py";
    private String delayedSendScript = "send_later.py";
    private String sendMode = "enter";
    private double waitSeconds = 8;
    private long timeoutSeconds = 90;
    private int defaultCountdownSeconds = 5;

    /**
     * 提醒时随附的二维码图片缓存目录。后端会把「假二维码 / 真二维码 PNG」写到这里，
     * 并把【绝对路径】放进 payload 交给 worker。开发环境 worker 和后端同机运行，
     * 可以直接读；生产阶段要跨机时再换成 URL 下载。
     */
    private String reminderQrCacheDir = "data/reminder-qr";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSdkRoot() {
        return sdkRoot;
    }

    public void setSdkRoot(String sdkRoot) {
        this.sdkRoot = sdkRoot;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getPythonCommand() {
        return pythonCommand;
    }

    public void setPythonCommand(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    public String getSendScript() {
        return sendScript;
    }

    public void setSendScript(String sendScript) {
        this.sendScript = sendScript;
    }

    public String getSendMode() {
        return sendMode;
    }

    public void setSendMode(String sendMode) {
        this.sendMode = sendMode;
    }

    public String getDelayedSendScript() {
        return delayedSendScript;
    }

    public void setDelayedSendScript(String delayedSendScript) {
        this.delayedSendScript = delayedSendScript;
    }

    public double getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(double waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getDefaultCountdownSeconds() {
        return defaultCountdownSeconds;
    }

    public void setDefaultCountdownSeconds(int defaultCountdownSeconds) {
        this.defaultCountdownSeconds = defaultCountdownSeconds;
    }

    public String getReminderQrCacheDir() {
        return reminderQrCacheDir;
    }

    public void setReminderQrCacheDir(String reminderQrCacheDir) {
        this.reminderQrCacheDir = reminderQrCacheDir;
    }
}
