package com.hospital.followup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.followup.common.ApiResponse;
import com.hospital.followup.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminAuthService adminAuthService, ObjectMapper objectMapper) {
        this.adminAuthService = adminAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/api/admin/auth/login")) {
            return true;
        }

        String token = extractToken(request.getHeader("Authorization"));
        if (adminAuthService.isTokenValid(token)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("未登录或登录已过期"));
        return false;
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
