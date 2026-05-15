package com.bazylev.server.network;

import com.bazylev.server.enums.Role;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Session {

    private final String token;
    private final int userId;
    private final String login;
    private final Role role;
    private volatile LocalDateTime lastActivity;

    public Session(String token, int userId, Role role) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.login = null;
        this.lastActivity = LocalDateTime.now();
    }

    public Session(String token, int userId, String login, Role role) {
        this.token = token;
        this.userId = userId;
        this.login = login;
        this.role = role;
        this.lastActivity = LocalDateTime.now();
    }

    public boolean isExpired(long ttlMinutes) {
        return ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now()) >= ttlMinutes;
    }

    public void refreshActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public String getToken() { return token; }
    public int getUserId() { return userId; }
    public String getLogin() { return login; }
    public Role getRole() { return role; }
    public LocalDateTime getLastActivity() { return lastActivity; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isTeacher() { return role == Role.TEACHER; }
    public boolean isStudent() { return role == Role.STUDENT; }

    public boolean hasAnyRole(Role... roles) {
        for (Role r : roles) {
            if (this.role == r) return true;
        }
        return false;
    }
}