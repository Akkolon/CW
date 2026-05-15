package com.bazylev.server.network;

import com.bazylev.server.enums.Role;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private static final long SESSION_TTL_MINUTES = 60;

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(int userId, String login, Role role) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(token, userId, login, role));
        return token;
    }

    public Optional<Session> getSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired(SESSION_TTL_MINUTES)) {
            sessions.remove(token);
            return Optional.empty();
        }
        session.refreshActivity();
        return Optional.of(session);
    }

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void evictExpired() {
        sessions.entrySet().removeIf(e -> e.getValue().isExpired(SESSION_TTL_MINUTES));
    }
}
