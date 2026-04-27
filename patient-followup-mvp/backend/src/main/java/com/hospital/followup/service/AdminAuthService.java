package com.hospital.followup.service;

import com.hospital.followup.config.AdminAuthProperties;
import com.hospital.followup.dto.admin.AdminUserView;
import com.hospital.followup.dto.admin.LoginRequest;
import com.hospital.followup.dto.admin.LoginResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminAuthProperties authProperties;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public AdminAuthService(AdminAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public LoginResponse login(LoginRequest request) {
        if (!authProperties.getUsername().equals(request.username()) || !authProperties.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(authProperties.getTokenTtlMinutes());
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionInfo(buildUser(), expiresAt));
        return new LoginResponse(token, expiresAt, buildUser());
    }

    public AdminUserView getCurrentUser(String token) {
        SessionInfo session = getValidSession(token);
        return session.user();
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    public boolean isTokenValid(String token) {
        return token != null && !token.isBlank() && getValidSession(token) != null;
    }

    private SessionInfo getValidSession(String token) {
        SessionInfo session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    private AdminUserView buildUser() {
        return new AdminUserView(authProperties.getUsername(), "系统管理员", "ADMIN");
    }

    private record SessionInfo(AdminUserView user, LocalDateTime expiresAt) {
    }
}
