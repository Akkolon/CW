package com.bazylev.server.integration;

import com.bazylev.server.enums.RequestType;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Server;
import com.google.gson.Gson;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Интеграционные тесты: клиент ↔ сервер")
class ServerIntegrationTest {

    private static final int TEST_PORT = 15555;
    private static final Gson GSON = GsonFactory.getInstance();

    private static Thread serverThread;

    @BeforeAll
    static void startServer() {
        serverThread = new Thread(() -> {
            Server server = new Server(TEST_PORT, 4);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Даём серверу время на запуск
        awaitServerStart();
    }

    // ----------------------------------------------------------------
    // Подключение
    // ----------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Клиент успешно подключается к серверу")
    void clientCanConnect() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            assertTrue(socket.isConnected());
        }
    }

    // ----------------------------------------------------------------
    // LOGIN — маршрутизация без БД
    // ----------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("LOGIN с пустыми данными: сервер возвращает ERROR, не падает")
    void login_emptyData_serverRespondsWithError() throws Exception {
        Request request = new Request(RequestType.LOGIN, null, "{}");
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("LOGIN с некорректным JSON в data: сервер отвечает ERROR")
    void login_malformedData_serverRespondsWithError() throws Exception {
        Request request = new Request(RequestType.LOGIN, null, "not-json");
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // Авторизация — защищённые эндпоинты
    // ----------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("GET_ALL_GROUPS без токена: сервер возвращает UNAUTHORIZED")
    void getAllGroups_noToken_unauthorized() throws Exception {
        Request request = new Request(RequestType.GET_ALL_GROUPS, null, null);
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("SET_GRADE без токена: сервер возвращает UNAUTHORIZED")
    void setGrade_noToken_unauthorized() throws Exception {
        Request request = new Request(RequestType.SET_GRADE, null,
                "{\"groupStudentId\":1,\"value\":8.0}");
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("REGISTER_PAYMENT без токена: сервер возвращает UNAUTHORIZED")
    void registerPayment_noToken_unauthorized() throws Exception {
        Request request = new Request(RequestType.REGISTER_PAYMENT, null,
                "{\"studentId\":1,\"amount\":1000}");
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Несуществующий токен: сервер возвращает UNAUTHORIZED")
    void request_fakeToken_unauthorized() throws Exception {
        Request request = new Request(RequestType.GET_ALL_STUDENTS,
                "fake-token-00000000", null);
        Response response = sendAndReceive(request);

        assertNotNull(response);
        assertEquals(ResponseStatus.UNAUTHORIZED, response.getStatus());
    }

    // ----------------------------------------------------------------
    // Формат запроса
    // ----------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("Некорректный JSON: сервер отвечает ERROR, соединение не разрывается")
    void invalidJson_serverRespondsAndStaysAlive() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out.println("this is not json at all");
            String raw = in.readLine();
            assertNotNull(raw);

            Response response = GSON.fromJson(raw, Response.class);
            assertEquals(ResponseStatus.ERROR, response.getStatus());

            // Второй запрос — соединение живо
            out.println("{}");
            raw = in.readLine();
            assertNotNull(raw);
        }
    }

    @Test
    @Order(9)
    @DisplayName("Пустая строка: сервер возвращает ERROR без падения")
    void emptyLine_serverRespondsWithError() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out.println("   ");
            String raw = in.readLine();
            assertNotNull(raw);

            Response response = GSON.fromJson(raw, Response.class);
            assertEquals(ResponseStatus.ERROR, response.getStatus());
        }
    }

    @Test
    @Order(10)
    @DisplayName("Несколько последовательных запросов в одном соединении")
    void multipleRequests_sameConnection_allAnswered() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            for (int i = 0; i < 3; i++) {
                Request req = new Request(RequestType.GET_ALL_GROUPS, null, null);
                out.println(GSON.toJson(req));
                String raw = in.readLine();
                assertNotNull(raw, "Ответ на запрос " + (i + 1) + " не получен");
                Response response = GSON.fromJson(raw, Response.class);
                assertNotNull(response.getStatus());
            }
        }
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    private Response sendAndReceive(Request request) throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out.println(GSON.toJson(request));
            String raw = in.readLine();
            return GSON.fromJson(raw, Response.class);
        }
    }

    private static void awaitServerStart() {
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(100);
                new Socket("localhost", TEST_PORT).close();
                return;
            } catch (Exception ignored) {}
        }
        throw new IllegalStateException("Сервер не запустился за 2 секунды");
    }
}
