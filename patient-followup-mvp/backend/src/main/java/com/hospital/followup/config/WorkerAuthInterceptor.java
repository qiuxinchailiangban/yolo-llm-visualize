package com.hospital.followup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WorkerAuthInterceptor implements HandlerInterceptor {

    private final AutomationWorkerProperties properties;
    private final ObjectMapper objectMapper;

    public WorkerAuthInterceptor(AutomationWorkerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = request.getHeader("X-Worker-Token");
        if (StringUtils.hasText(token) && token.equals(properties.getToken())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("worker token 无效"));
        return false;
    }
}
