package com.hospital.followup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.config.WechatMiniappProperties;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class WechatMiniappService {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniappService.class);

    private final WechatMiniappProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private String cachedAccessToken;
    private LocalDateTime accessTokenExpiresAt;

    public WechatMiniappService(WechatMiniappProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.accessTokenExpiresAt = LocalDateTime.MIN;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getAppId()) && StringUtils.hasText(properties.getAppSecret());
    }

    public String getAppId() {
        return properties.getAppId();
    }

    public String getEnvVersion() {
        return properties.getEnvVersion();
    }

    public String getPagePath() {
        return properties.getPagePath();
    }

    public String getPublicApiBaseUrl() {
        return properties.getPublicApiBaseUrl();
    }

    /**
     * 仅供自检使用：尝试拉取 access_token 并把异常转成普通字符串结果，便于一次性显示给运营。
     */
    public AccessTokenProbeResult probeAccessToken() {
        if (!isConfigured()) {
            return AccessTokenProbeResult.fail("未配置 app-id / app-secret");
        }
        try {
            String token = getAccessToken();
            if (StringUtils.hasText(token)) {
                return AccessTokenProbeResult.ok("成功拿到 access_token，长度 " + token.length());
            }
            return AccessTokenProbeResult.fail("微信返回 access_token 为空");
        } catch (Exception error) {
            log.warn("[self-check] 获取微信 access_token 失败", error);
            return AccessTokenProbeResult.fail(error.getMessage() == null ? "未知错误" : error.getMessage());
        }
    }

    public record AccessTokenProbeResult(boolean ok, String message) {
        public static AccessTokenProbeResult ok(String message) {
            return new AccessTokenProbeResult(true, message);
        }

        public static AccessTokenProbeResult fail(String message) {
            return new AccessTokenProbeResult(false, message);
        }
    }

    public byte[] generateMiniProgramCode(String token) {
        if (!isConfigured()) {
            throw new IllegalStateException("未配置微信小程序 appId/appSecret，暂时无法生成官方小程序码");
        }

        Map<String, Object> payload = Map.of(
            "scene", token,
            "page", properties.getPagePath(),
            "env_version", properties.getEnvVersion(),
            "check_path", false
        );

        // 用 exchange 自己处理状态码：不让 RestClient 在 4xx/5xx 时抛异常，
        // 微信有时会在请求头或 body 不对的情况下返回 412 / 415 / 空 body，我们要拿到 body 再判断。
        return restClient.post()
            .uri(
                "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token={accessToken}",
                getAccessToken()
            )
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .exchange(
                (request, response) -> {
                    byte[] bodyBytes;
                    try {
                        bodyBytes = response.getBody() == null
                            ? new byte[0]
                            : StreamUtils.copyToByteArray(response.getBody());
                    } catch (Exception readError) {
                        bodyBytes = new byte[0];
                        log.warn("[wechat] 读取小程序码响应体失败", readError);
                    }

                    int statusCode = response.getStatusCode().value();

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        String rawPreview = previewBody(bodyBytes);
                        log.warn(
                            "[wechat] getwxacodeunlimit 非 2xx 响应, status={} bodyPreview={}",
                            statusCode, rawPreview
                        );
                        if (looksLikeJson(bodyBytes)) {
                            throw new IllegalStateException(parseWechatError(bodyBytes));
                        }
                        throw new IllegalStateException(
                            buildHttpErrorHint(statusCode, rawPreview)
                        );
                    }

                    if (bodyBytes.length == 0) {
                        throw new IllegalStateException("微信小程序码接口未返回图片数据");
                    }
                    if (looksLikeJson(bodyBytes)) {
                        throw new IllegalStateException(parseWechatError(bodyBytes));
                    }
                    return bodyBytes;
                },
                true
            );
    }

    private String previewBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "<empty>";
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    private String buildHttpErrorHint(int statusCode, String rawPreview) {
        StringBuilder sb = new StringBuilder();
        sb.append("微信小程序码接口返回 HTTP ").append(statusCode);
        if (!"<empty>".equals(rawPreview)) {
            sb.append(", body=").append(rawPreview);
        } else {
            sb.append("（空 body）");
        }
        if (statusCode == 412 || statusCode == 415) {
            sb.append("。最常见原因：")
                .append("(1) env_version=")
                .append(properties.getEnvVersion())
                .append(" 的小程序还没在微信后台上传对应版本；")
                .append("(2) page-path=")
                .append(properties.getPagePath())
                .append(" 在该版本里不存在；")
                .append("(3) appSecret 不对或 access_token 被限频。")
                .append("先在 mp.weixin.qq.com 把小程序上传成体验版，再把自己加为体验成员，然后重试。");
        } else if (statusCode == 401 || statusCode == 403) {
            sb.append("。检查 appId / appSecret 是否正确，或 IP 是否在白名单内。");
        }
        return sb.toString();
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && accessTokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(1))) {
            return cachedAccessToken;
        }

        JsonNode response = restClient.get()
            .uri(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={secret}",
                properties.getAppId(),
                properties.getAppSecret()
            )
            .retrieve()
            .body(JsonNode.class);

        if (response == null || !response.hasNonNull("access_token")) {
            throw new IllegalStateException("获取微信 access_token 失败");
        }

        cachedAccessToken = response.get("access_token").asText();
        long expiresIn = response.path("expires_in").asLong(7200);
        accessTokenExpiresAt = LocalDateTime.now().plusSeconds(Math.max(300, expiresIn - 300));
        return cachedAccessToken;
    }

    private boolean looksLikeJson(byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8).trim();
        return value.startsWith("{") && value.endsWith("}");
    }

    private String parseWechatError(byte[] bytes) {
        try {
            JsonNode jsonNode = objectMapper.readTree(bytes);
            String message = jsonNode.path("errmsg").asText("微信接口返回错误");
            int errorCode = jsonNode.path("errcode").asInt(-1);
            return errorCode >= 0 ? "微信小程序码生成失败: " + message + " (" + errorCode + ")" : message;
        } catch (Exception error) {
            return "微信小程序码生成失败";
        }
    }
}
