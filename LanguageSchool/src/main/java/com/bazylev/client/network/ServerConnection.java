package com.bazylev.client.network;

import com.bazylev.client.models.tcp.Request;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.session.ClientSession;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public final class ServerConnection {

    private static final ServerConnection INSTANCE = new ServerConnection();

    private final String host;
    private final int port;
    private final Gson gson = GsonFactory.getInstance();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private ServerConnection() {
        Properties props = loadConfig();
        this.host = props.getProperty("server.host", "localhost");
        this.port = Integer.parseInt(props.getProperty("server.port", "5555"));
    }

    public static ServerConnection getInstance() {
        return INSTANCE;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public Response send(Request request) {
        if (!isConnected()) {
            return Response.error("Нет соединения с сервером");
        }

        if (request.getRequestType() != RequestType.LOGIN
                && request.getRequestType() != RequestType.LOGOUT
                && request.getToken() == null) {
            String token = ClientSession.getInstance().getToken();
            if (token != null && !token.isBlank()) {
                request.setToken(token);
            }
        }

        try {
            out.println(gson.toJson(request));
            String raw = in.readLine();
            if (raw == null) {
                return Response.error("Сервер закрыл соединение");
            }
            return gson.fromJson(raw, Response.class);
        } catch (IOException e) {
            return Response.error("Ошибка связи с сервером: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) props.load(input);
        } catch (IOException e) {
            System.err.println("Не удалось загрузить config.properties: " + e.getMessage());
        }
        return props;
    }
}
