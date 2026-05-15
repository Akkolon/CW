package com.bazylev.server.services;

import com.bazylev.server.config.AppConfig;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthService {

    private static final int MAX_ATTEMPTS;
    private static final long LOCKOUT_MILLIS;

    static {
        AppConfig cfg = AppConfig.getInstance();
        MAX_ATTEMPTS   = cfg.getInt("auth.max.attempts");
        LOCKOUT_MILLIS = cfg.getLong("auth.lockout.minutes") * 60_000L;
    }

    private final UserDAO        userDAO    = new UserDAO();
    private final StudentDAO     studentDAO = new StudentDAO();
    private final SessionManager sessionMgr = SessionManager.getInstance();
    private final Gson           gson       = GsonFactory.getInstance();

    private final Map<String, AtomicInteger> failCounters  = new ConcurrentHashMap<>();
    private final Map<String, Long>          lockoutExpiry = new ConcurrentHashMap<>();

    public Response login(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные для входа не переданы");
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String login    = json.has("login")    ? json.get("login").getAsString()    : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";

        if (login.isBlank() || password.isBlank()) {
            return Response.error("Логин и пароль не могут быть пустыми");
        }

        if (isLockedOut(login)) {
            long remaining = (lockoutExpiry.get(login) - System.currentTimeMillis()) / 1000;
            return Response.error("Аккаунт временно заблокирован. Повторите через " + remaining + " сек.");
        }

        Optional<User> userOpt = userDAO.findByLogin(login);

        if (userOpt.isEmpty()) {
            registerFailedAttempt(login);
            return Response.error("Неверный логин или пароль");
        }

        User user = userOpt.get();

        if (user.isBlocked()) {
            return Response.error("Учётная запись заблокирована администратором");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            registerFailedAttempt(login);
            int remaining = MAX_ATTEMPTS - failCounters.get(login).get();
            if (remaining <= 0) {
                return Response.error("Неверный логин или пароль. Аккаунт заблокирован на "
                        + AppConfig.getInstance().getInt("auth.lockout.minutes") + " мин.");
            }
            return Response.error("Неверный логин или пароль. Осталось попыток: " + remaining);
        }

        resetFailedAttempts(login);

        String token = sessionMgr.createSession(user.getId(), user.getLogin(), user.getRole());

        JsonObject responseData = new JsonObject();
        responseData.addProperty("token",  token);
        responseData.addProperty("role",   user.getRole().name());
        responseData.addProperty("login",  user.getLogin());
        responseData.addProperty("userId", user.getId());

        // Для студентов добавляем studentId — он нужен клиенту
        // чтобы запрашивать оценки, посещаемость и расписание
        if (user.getRole() == Role.STUDENT) {
            studentDAO.findAll().stream()
                    .filter(s -> s.getPersonId() == user.getPersonId())
                    .findFirst()
                    .ifPresent(s -> responseData.addProperty("studentId", s.getId()));
        }

        return Response.ok(responseData.toString());
    }

    private boolean isLockedOut(String login) {
        Long expiry = lockoutExpiry.get(login);
        if (expiry == null) return false;
        if (System.currentTimeMillis() < expiry) return true;
        lockoutExpiry.remove(login);
        failCounters.remove(login);
        return false;
    }

    private void registerFailedAttempt(String login) {
        AtomicInteger counter = failCounters.computeIfAbsent(login, k -> new AtomicInteger(0));
        int attempts = counter.incrementAndGet();
        if (attempts >= MAX_ATTEMPTS) {
            lockoutExpiry.put(login, System.currentTimeMillis() + LOCKOUT_MILLIS);
        }
    }

    private void resetFailedAttempts(String login) {
        failCounters.remove(login);
        lockoutExpiry.remove(login);
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}
