package com.bazylev.server.commands.user;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.UserService;

public class CreateUserCommand implements Command {

    private final UserService userService;

    public CreateUserCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может создавать пользователей");
        }
        return userService.createUser(request.getData());
    }
}
