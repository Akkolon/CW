package com.bazylev.server.commands.user;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.UserService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BlockUserCommand implements Command {

    private final UserService userService;

    public BlockUserCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может блокировать пользователей");
        }

        try {
            JsonObject json = JsonParser.parseString(request.getData()).getAsJsonObject();
            int targetUserId = json.has("userId") ? json.get("userId").getAsInt() : -1;
            boolean blocking = json.has("blocked") && json.get("blocked").getAsBoolean();

            if (blocking && targetUserId == session.getUserId()) {
                return Response.error("Администратор не может заблокировать сам себя");
            }
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        return userService.setBlocked(request.getData());
    }
}
