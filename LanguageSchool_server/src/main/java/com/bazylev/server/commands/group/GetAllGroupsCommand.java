package com.bazylev.server.commands.group;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.GroupService;

public class GetAllGroupsCommand implements Command {

    private final GroupService groupService;

    public GetAllGroupsCommand(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return groupService.getAllGroups(session);
    }
}
