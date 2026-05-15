package com.bazylev.server.commands.auth;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.network.SessionManager;

public class LogoutCommand implements Command {

    private final SessionManager sessionManager;

    public LogoutCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response execute(Request request, Session session) {
        sessionManager.invalidate(request.getToken());
        return Response.ok();
    }
}
