package com.bazylev.server.services;

import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — модульные тесты")
class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private SessionManager sessionManager;

    private AuthService authService;

    private static final String PLAIN_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = BCrypt.hashpw(PLAIN_PASSWORD, BCrypt.gensalt(4));

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService();
        injectMock(authService, "userDAO", userDAO);
        injectMock(authService, "sessionMgr", sessionManager);
    }

    // ----------------------------------------------------------------
    // hashPassword — статический метод
    // ----------------------------------------------------------------

    @Test
    @DisplayName("hashPassword: хеш отличается от исходного пароля")
    void hashPassword_producesHash() {
        String hash = AuthService.hashPassword("secret");
        assertNotEquals("secret", hash);
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    @DisplayName("hashPassword: один пароль — разные хеши при каждом вызове")
    void hashPassword_differentSalts() {
        String hash1 = AuthService.hashPassword("secret");
        String hash2 = AuthService.hashPassword("secret");
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashPassword: BCrypt.checkpw подтверждает корректность хеша")
    void hashPassword_verifiedByBcrypt() {
        String hash = AuthService.hashPassword("myPass");
        assertTrue(BCrypt.checkpw("myPass", hash));
        assertFalse(BCrypt.checkpw("wrongPass", hash));
    }

    // ----------------------------------------------------------------
    // login — базовые сценарии
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login: корректные данные — возвращает OK с токеном")
    void login_validCredentials_returnsToken() {
        User user = buildUser(1, "admin", HASHED_PASSWORD, Role.ADMIN, false);
        when(userDAO.findByLogin("admin")).thenReturn(Optional.of(user));
        when(sessionManager.createSession(1, "admin", Role.ADMIN)).thenReturn("test-token");

        Response response = authService.login("{\"login\":\"admin\",\"password\":\"password123\"}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        assertNotNull(response.getData());
        assertTrue(response.getData().contains("token"));
        verify(sessionManager).createSession(1, "admin", Role.ADMIN);
    }

    @Test
    @DisplayName("login: неверный пароль — возвращает ERROR")
    void login_wrongPassword_returnsError() {
        User user = buildUser(1, "admin", HASHED_PASSWORD, Role.ADMIN, false);
        when(userDAO.findByLogin("admin")).thenReturn(Optional.of(user));

        Response response = authService.login("{\"login\":\"admin\",\"password\":\"wrong\"}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(sessionManager, never()).createSession(anyInt(), anyString(), any());
    }

    @Test
    @DisplayName("login: несуществующий пользователь — возвращает ERROR")
    void login_unknownUser_returnsError() {
        when(userDAO.findByLogin("ghost")).thenReturn(Optional.empty());

        Response response = authService.login("{\"login\":\"ghost\",\"password\":\"any\"}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("login: заблокированный пользователь — возвращает ERROR")
    void login_blockedUser_returnsError() {
        User user = buildUser(2, "blocked", HASHED_PASSWORD, Role.STUDENT, true);
        when(userDAO.findByLogin("blocked")).thenReturn(Optional.of(user));

        Response response = authService.login("{\"login\":\"blocked\",\"password\":\"password123\"}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("заблокирована"));
        verify(sessionManager, never()).createSession(anyInt(), anyString(), any());
    }

    @Test
    @DisplayName("login: пустой логин — возвращает ERROR")
    void login_emptyLogin_returnsError() {
        Response response = authService.login("{\"login\":\"\",\"password\":\"pass\"}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("login: пустой пароль — возвращает ERROR")
    void login_emptyPassword_returnsError() {
        Response response = authService.login("{\"login\":\"admin\",\"password\":\"\"}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("login: null данные — возвращает ERROR")
    void login_nullData_returnsError() {
        Response response = authService.login(null);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("login: некорректный JSON — возвращает ERROR")
    void login_invalidJson_returnsError() {
        Response response = authService.login("not-a-json");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // login — блокировка после 3 неудачных попыток
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login: после 3 неудачных попыток аккаунт блокируется")
    void login_threeFailedAttempts_accountLockedOut() {
        User user = buildUser(3, "testuser", HASHED_PASSWORD, Role.STUDENT, false);
        when(userDAO.findByLogin("testuser")).thenReturn(Optional.of(user));

        String payload = "{\"login\":\"testuser\",\"password\":\"wrong\"}";

        authService.login(payload);
        authService.login(payload);
        Response thirdAttempt = authService.login(payload);

        assertEquals(ResponseStatus.ERROR, thirdAttempt.getStatus());

        Response afterLockout = authService.login(payload);
        assertEquals(ResponseStatus.ERROR, afterLockout.getStatus());
        assertTrue(afterLockout.getMessage().contains("заблокирован"));
    }

    @Test
    @DisplayName("login: успешный вход сбрасывает счётчик неудачных попыток")
    void login_successResetsFailCounter() throws Exception {
        User user = buildUser(4, "user2", HASHED_PASSWORD, Role.TEACHER, false);
        when(userDAO.findByLogin("user2")).thenReturn(Optional.of(user));

        Field sessionMgrField = AuthService.class.getDeclaredField("sessionMgr");
        sessionMgrField.setAccessible(true);
        SessionManager originalSessionMgr = (SessionManager) sessionMgrField.get(authService);
        sessionMgrField.set(authService, sessionManager);
        when(sessionManager.createSession(4, "user2", Role.TEACHER)).thenReturn("tok");

        Field failCountersField = AuthService.class.getDeclaredField("failCounters");
        failCountersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, AtomicInteger> failCounters = (Map<String, AtomicInteger>) failCountersField.get(authService);

        authService.login("{\"login\":\"user2\",\"password\":\"wrong\"}");
        authService.login("{\"login\":\"user2\",\"password\":\"wrong\"}");
        assertEquals(2, failCounters.get("user2").get());

        authService.login("{\"login\":\"user2\",\"password\":\"password123\"}");

        assertNull(failCounters.get("user2"));

        sessionMgrField.set(authService, originalSessionMgr);
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    private User buildUser(int id, String login, String hash, Role role, boolean blocked) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setPasswordHash(hash);
        user.setRole(role);
        user.setBlocked(blocked);
        user.setPersonId(id);
        return user;
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }
}
