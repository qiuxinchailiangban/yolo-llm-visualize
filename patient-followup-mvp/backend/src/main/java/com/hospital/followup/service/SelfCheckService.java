package com.hospital.followup.service;

import com.hospital.followup.config.AutomationWorkerProperties;
import com.hospital.followup.config.CorsProperties;
import com.hospital.followup.config.RpaIntegrationProperties;
import com.hospital.followup.dto.admin.SelfCheckView;
import com.hospital.followup.dto.admin.SelfCheckView.SelfCheckItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SelfCheckService {

    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARN = "WARN";
    private static final String LEVEL_OK = "OK";

    private final WechatMiniappService wechatMiniappService;
    private final CorsProperties corsProperties;
    private final RpaIntegrationProperties rpaProperties;
    private final AutomationWorkerProperties workerProperties;

    public SelfCheckService(
        WechatMiniappService wechatMiniappService,
        CorsProperties corsProperties,
        RpaIntegrationProperties rpaProperties,
        AutomationWorkerProperties workerProperties
    ) {
        this.wechatMiniappService = wechatMiniappService;
        this.corsProperties = corsProperties;
        this.rpaProperties = rpaProperties;
        this.workerProperties = workerProperties;
    }

    public SelfCheckView runFullCheck() {
        List<SelfCheckItem> items = new ArrayList<>();
        items.add(checkWechatConfigured());
        items.add(checkWechatAccessToken());
        items.add(checkPublicApiBaseUrl());
        items.add(checkCors());
        items.add(checkRpa());
        items.add(checkWorkerToken());

        boolean overallOk = items.stream().allMatch(item -> item.ok() || LEVEL_WARN.equals(item.level()));
        return new SelfCheckView(overallOk, items);
    }

    private SelfCheckItem checkWechatConfigured() {
        if (wechatMiniappService.isConfigured()) {
            return new SelfCheckItem(
                "wechat.configured",
                "微信小程序 appId / appSecret",
                LEVEL_OK,
                true,
                "已配置 appId = " + mask(wechatMiniappService.getAppId()),
                null
            );
        }
        return new SelfCheckItem(
            "wechat.configured",
            "微信小程序 appId / appSecret",
            LEVEL_ERROR,
            false,
            "未配置",
            "请在 application-local.yml 里填入 app.wechat-miniapp.app-id 和 app-secret，并以 mysql,local 双 profile 启动"
        );
    }

    private SelfCheckItem checkWechatAccessToken() {
        WechatMiniappService.AccessTokenProbeResult result = wechatMiniappService.probeAccessToken();
        if (result.ok()) {
            return new SelfCheckItem(
                "wechat.access_token",
                "微信 access_token 联通性",
                LEVEL_OK,
                true,
                result.message(),
                null
            );
        }
        return new SelfCheckItem(
            "wechat.access_token",
            "微信 access_token 联通性",
            LEVEL_ERROR,
            false,
            result.message(),
            "appId/appSecret 错误，或服务器无法访问 https://api.weixin.qq.com（检查防火墙/代理）"
        );
    }

    private SelfCheckItem checkPublicApiBaseUrl() {
        String url = wechatMiniappService.getPublicApiBaseUrl();
        if (!StringUtils.hasText(url)) {
            return new SelfCheckItem(
                "public.base_url",
                "对外公网域名 (public-api-base-url)",
                LEVEL_ERROR,
                false,
                "未配置",
                "在 application-local.yml 里设置 app.wechat-miniapp.public-api-base-url 为公网 HTTPS 地址，例如 https://xxx.cpolar.cn"
            );
        }
        boolean isLocalhost = url.contains("localhost") || url.contains("127.0.0.1");
        boolean isHttps = url.startsWith("https://");
        if (isLocalhost) {
            return new SelfCheckItem(
                "public.base_url",
                "对外公网域名 (public-api-base-url)",
                LEVEL_WARN,
                false,
                "当前是 " + url + "，仅自己电脑可访问",
                "想让别人手机扫码可用，必须改成公网 HTTPS 域名（cpolar / 自有域名 + nginx）"
            );
        }
        if (!isHttps) {
            return new SelfCheckItem(
                "public.base_url",
                "对外公网域名 (public-api-base-url)",
                LEVEL_ERROR,
                false,
                "当前是 " + url + "，不是 HTTPS",
                "微信小程序强制要求 HTTPS，请加证书或换成 https://"
            );
        }
        return new SelfCheckItem(
            "public.base_url",
            "对外公网域名 (public-api-base-url)",
            LEVEL_OK,
            true,
            "已配置 " + url,
            "记得把这个域名也加到微信公众平台 → 开发管理 → 服务器域名 → request 合法域名"
        );
    }

    private SelfCheckItem checkCors() {
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return new SelfCheckItem(
                "cors.allowed_origins",
                "管理端 CORS 允许来源",
                LEVEL_WARN,
                false,
                "未配置任何允许来源",
                "至少加上 http://localhost:5173（管理端 dev）和你的管理端正式域名"
            );
        }
        return new SelfCheckItem(
            "cors.allowed_origins",
            "管理端 CORS 允许来源",
            LEVEL_OK,
            true,
            "已允许 " + origins,
            null
        );
    }

    private SelfCheckItem checkRpa() {
        if (!rpaProperties.isEnabled()) {
            return new SelfCheckItem(
                "rpa.enabled",
                "RPA 集成",
                LEVEL_WARN,
                false,
                "未启用",
                "如果还不需要发微信提醒可以先忽略；要发提醒需在配置里开 app.rpa-integration.enabled=true 并启动 desktop-worker"
            );
        }
        if (!StringUtils.hasText(rpaProperties.getSdkRoot())) {
            return new SelfCheckItem(
                "rpa.enabled",
                "RPA 集成",
                LEVEL_ERROR,
                false,
                "已启用但未配置 sdk-root",
                "在 application-local.yml 设置 app.rpa-integration.sdk-root 为 omni_bot_sdk-1.0.6 的本地路径"
            );
        }
        return new SelfCheckItem(
            "rpa.enabled",
            "RPA 集成",
            LEVEL_OK,
            true,
            "已启用，sdk-root=" + rpaProperties.getSdkRoot(),
            null
        );
    }

    private SelfCheckItem checkWorkerToken() {
        String token = workerProperties.getToken();
        if (!StringUtils.hasText(token)) {
            return new SelfCheckItem(
                "worker.token",
                "desktop-worker 共享 token",
                LEVEL_ERROR,
                false,
                "未配置",
                "在 application-local.yml 设置 app.automation-worker.token，desktop-worker/config.json 必须填同样的值"
            );
        }
        return new SelfCheckItem(
            "worker.token",
            "desktop-worker 共享 token",
            LEVEL_OK,
            true,
            "已配置（" + mask(token) + "），desktop-worker/config.json 必须填一致的值",
            null
        );
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "(空)";
        }
        if (value.length() <= 6) {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }
}
