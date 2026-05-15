package com.bazylev.server.network;

import com.bazylev.server.commands.CommandDispatcher;
import com.bazylev.server.commands.CommandRegistry;
import com.bazylev.server.enums.RequestType;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Gson GSON = GsonFactory.getInstance();

    private final Socket socket;
    private final CommandDispatcher dispatcher;
    private final SessionManager sessionManager;
    private String clientAddress;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = CommandRegistry.build();
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public void run() {
        clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                Response response = handleLine(line);
                out.println(GSON.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("[-] Ошибка соединения с " + clientAddress + ": " + e.getMessage());
        } finally {
            closeQuietly();
            System.out.println("[-] Отключился клиент: " + clientAddress);
        }
    }

    private Response handleLine(String line) {
        if (line.isBlank()) {
            return Response.error("Получен пустой запрос");
        }

        Request request;
        try {
            request = GSON.fromJson(line, Request.class);
        } catch (JsonSyntaxException e) {
            return Response.error("Некорректный формат JSON: " + e.getMessage());
        }

        if (request.getRequestType() == null) {
            System.err.println("Не удалось распознать requestType. Запрос от " + clientAddress + ": " + line);
            return Response.error("Не указан тип запроса");
        }

        Session session = resolveSession(request);

        if (requiresAuth(request.getRequestType()) && session == null) {
            return Response.unauthorized();
        }

        try {
            return dispatcher.dispatch(request, session);
        } catch (Exception e) {
            System.err.println("Ошибка выполнения команды "
                    + request.getRequestType() + ": " + e.getMessage());
            return Response.error("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    private Session resolveSession(Request request) {
        return sessionManager.getSession(request.getToken()).orElse(null);
    }

    private boolean requiresAuth(RequestType type) {
        return type != RequestType.LOGIN
                && type != RequestType.REGISTER_STUDENT;
    }

    private void closeQuietly() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка закрытия сокета: " + e.getMessage());
        }
    }
}