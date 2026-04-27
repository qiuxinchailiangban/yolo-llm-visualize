package com.hospital.followup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.automation-worker")
public class AutomationWorkerProperties {

    private String token = "followup-worker-token";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
