package com.bazylev.server.commands.user;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.UserService;

public class GetActionLogCommand implements Command {

    private final UserService userService;

    public GetActionLogCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может просматривать журнал действий");
        }
        return userService.getActionLog(request.getData());
    }
}
