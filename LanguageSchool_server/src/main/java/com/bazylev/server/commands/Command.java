package com.bazylev.server.commands;

import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;

public interface Command {
    Response execute(Request request, Session session);
}
