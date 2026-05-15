package com.bazylev.server.commands;

import com.bazylev.server.enums.RequestType;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;

import java.util.EnumMap;
import java.util.Map;

public class CommandDispatcher {

    private final Map<RequestType, Command> registry = new EnumMap<>(RequestType.class);

    public void register(RequestType type, Command command) {
        registry.put(type, command);
    }

    public Response dispatch(Request request, Session session) {
        Command command = registry.get(request.getRequestType());
        if (command == null) {
            return Response.error("Неизвестный тип запроса: " + request.getRequestType());
        }
        return command.execute(request, session);
    }

    public boolean hasCommand(RequestType type) {
        return registry.containsKey(type);
    }
}
