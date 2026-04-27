package com.hospital.followup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wechat-miniapp")
public class WechatMiniappProperties {

    private String appId;
    private String appSecret;
    private String pagePath = "pages/questionnaire/index";
    private String envVersion = "trial";
    private String publicApiBaseUrl = "http://localhost:8080";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getEnvVersion() {
        return envVersion;
    }

    public void setEnvVersion(String envVersion) {
        this.envVersion = envVersion;
    }

    public String getPublicApiBaseUrl() {
        return publicApiBaseUrl;
    }

    public void setPublicApiBaseUrl(String publicApiBaseUrl) {
        this.publicApiBaseUrl = publicApiBaseUrl;
    }
}
