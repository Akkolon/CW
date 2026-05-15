package com.bazylev.server.network;

import com.bazylev.server.commands.Command;
import com.bazylev.server.commands.CommandDispatcher;
import com.bazylev.server.enums.RequestType;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientHandler — модульные тесты обработки запросов")
class ClientHandlerTest {

    @Mock
    private CommandDispatcher dispatcher;

    @Mock
    private SessionManager sessionManager;

    private ClientHandlerTestable handler;
    private static final Gson GSON = GsonFactory.getInstance();

    @BeforeEach
    void setUp() throws Exception {
        handler = new ClientHandlerTestable();
        injectMock(handler, "dispatcher",    dispatcher);
        injectMock(handler, "sessionManager", sessionManager);
    }

    // ----------------------------------------------------------------
    // requiresAuth
    // ----------------------------------------------------------------

    @Test
    @DisplayName("LOGIN не требует авторизации")
    void requiresAuth_login_returnsFalse() throws Exception {
        assertFalse(invokeRequiresAuth(RequestType.LOGIN));
    }

    @Test
    @DisplayName("Все остальные типы требуют авторизации")
    void requiresAuth_otherTypes_returnTrue() throws Exception {
        assertTrue(invokeRequiresAuth(RequestType.GET_ALL_GROUPS));
        assertTrue(invokeRequiresAuth(RequestType.SET_GRADE));
        assertTrue(invokeRequiresAuth(RequestType.REGISTER_PAYMENT));
        assertTrue(invokeRequiresAuth(RequestType.GENERATE_REPORT));
    }

    // ----------------------------------------------------------------
    // handleLine — парсинг и маршрутизация
    // ----------------------------------------------------------------

    @Test
    @DisplayName("handleLine: пустая строка — ERROR без обращения к dispatcher")
    void handleLine_emptyLine_returnsError() throws Exception {
        Response response = invokeHandleLine("   ");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("handleLine: некорректный JSON — ERROR")
    void handleLine_invalidJson_returnsError() throws Exception {
        Response response = invokeHandleLine("not-a-json");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("handleLine: нет типа запроса — ERROR")
    void handleLine_missingRequestType_returnsError() throws Exception {
        Response response = invokeHandleLine("{\"token\":\"abc\",\"data\":\"{}\"}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("handleLine: LOGIN без токена — передаётся в dispatcher")
    void handleLine_loginRequest_passedToDispatcher() throws Exception {
        Request request = new Request(RequestType.LOGIN, null,
                "{\"login\":\"admin\",\"password\":\"pass\"}");
        Response expected = Response.ok("{\"token\":\"tok123\"}");

        when(dispatcher.dispatch(any(Request.class), isNull())).thenReturn(expected);

        Response response = invokeHandleLine(GSON.toJson(request));

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(dispatcher).dispatch(any(Request.class), isNull());
    }

    @Test
    @DisplayName("handleLine: запрос с невалидным токеном — UNAUTHORIZED")
    void handleLine_invalidToken_returnsUnauthorized() throws Exception {
        Request request = new Request(RequestType.GET_ALL_GROUPS, "bad-token", null);

        when(sessionManager.getSession("bad-token")).thenReturn(java.util.Optional.empty());

        Response response = invokeHandleLine(GSON.toJson(request));

        assertEquals(ResponseStatus.UNAUTHORIZED, response.getStatus());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("handleLine: валидный токен — запрос доходит до dispatcher")
    void handleLine_validToken_dispatchCalled() throws Exception {
        Session session = new Session("valid-tok", 1, "admin", Role.ADMIN);
        Request request = new Request(RequestType.GET_ALL_GROUPS, "valid-tok", null);

        when(sessionManager.getSession("valid-tok")).thenReturn(java.util.Optional.of(session));
        when(dispatcher.dispatch(any(Request.class), eq(session))).thenReturn(Response.ok("[]"));

        Response response = invokeHandleLine(GSON.toJson(request));

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(dispatcher).dispatch(any(Request.class), eq(session));
    }

    @Test
    @DisplayName("handleLine: dispatcher выбрасывает исключение — возвращается ERROR")
    void handleLine_dispatcherThrows_returnsError() throws Exception {
        Session session = new Session("tok", 1, "admin", Role.ADMIN);
        Request request = new Request(RequestType.GET_ALL_GROUPS, "tok", null);

        when(sessionManager.getSession("tok")).thenReturn(java.util.Optional.of(session));
        when(dispatcher.dispatch(any(), any())).thenThrow(new RuntimeException("DB error"));

        Response response = invokeHandleLine(GSON.toJson(request));

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("Внутренняя ошибка"));
    }

    // ----------------------------------------------------------------
    // CommandDispatcher — юнит-тест диспетчера отдельно
    // ----------------------------------------------------------------

    @Test
    @DisplayName("CommandDispatcher: неизвестный тип запроса — ERROR")
    void dispatcher_unknownType_returnsError() {
        CommandDispatcher realDispatcher = new CommandDispatcher();

        Request request = new Request(RequestType.GET_ALL_GROUPS, null, null);
        Response response = realDispatcher.dispatch(request, null);

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("Неизвестный"));
    }

    @Test
    @DisplayName("CommandDispatcher: зарегистрированная команда вызывается")
    void dispatcher_registeredCommand_called() {
        CommandDispatcher realDispatcher = new CommandDispatcher();
        Command mockCommand = mock(Command.class);
        when(mockCommand.execute(any(), any())).thenReturn(Response.ok("data"));

        realDispatcher.register(RequestType.GET_ALL_GROUPS, mockCommand);

        Request request = new Request(RequestType.GET_ALL_GROUPS, null, null);
        Response response = realDispatcher.dispatch(request, null);

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(mockCommand).execute(any(), any());
    }

    // ----------------------------------------------------------------
    // Session — вспомогательные методы
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Session.hasAnyRole: корректно проверяет роли")
    void session_hasAnyRole() {
        Session adminSession   = new Session("t", 1, "admin",   Role.ADMIN);
        Session teacherSession = new Session("t", 2, "teacher", Role.TEACHER);
        Session studentSession = new Session("t", 3, "student", Role.STUDENT);

        assertTrue(adminSession.hasAnyRole(Role.ADMIN));
        assertTrue(adminSession.hasAnyRole(Role.ADMIN, Role.TEACHER));
        assertFalse(adminSession.hasAnyRole(Role.TEACHER, Role.STUDENT));

        assertTrue(teacherSession.hasAnyRole(Role.ADMIN, Role.TEACHER));
        assertFalse(teacherSession.hasAnyRole(Role.ADMIN));

        assertTrue(studentSession.isStudent());
        assertFalse(studentSession.isAdmin());
    }

    @Test
    @DisplayName("Session.isExpired: истекает корректно по TTL")
    void session_isExpired() throws Exception {
        Session session = new Session("t", 1, "user", Role.ADMIN);

        assertFalse(session.isExpired(30));

        Field lastActivity = Session.class.getDeclaredField("lastActivity");
        lastActivity.setAccessible(true);
        lastActivity.set(session, java.time.LocalDateTime.now().minusMinutes(31));

        assertTrue(session.isExpired(30));
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    private Response invokeHandleLine(String line) throws Exception {
        Method method = ClientHandlerTestable.class
                .getSuperclass().getDeclaredMethod("handleLine", String.class);
        method.setAccessible(true);
        return (Response) method.invoke(handler, line);
    }

    private boolean invokeRequiresAuth(RequestType type) throws Exception {
        Method method = ClientHandlerTestable.class
                .getSuperclass().getDeclaredMethod("requiresAuth", RequestType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(handler, type);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Class<?> clazz = target.getClass().getSuperclass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    /**
     * Подкласс ClientHandler без сокета — для тестирования внутренних методов.
     */
    static class ClientHandlerTestable extends ClientHandler {
        ClientHandlerTestable() throws Exception {
            super(null);
        }
    }
}
