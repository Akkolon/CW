package com.bazylev.server.commands.auth;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.AuthService;

public class LoginCommand implements Command {

    private final AuthService authService;

    public LoginCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return authService.login(request.getData());
    }
}
