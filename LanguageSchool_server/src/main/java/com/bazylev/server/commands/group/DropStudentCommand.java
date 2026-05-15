package com.bazylev.server.commands.group;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.GroupService;

public class DropStudentCommand implements Command {

    private final GroupService groupService;

    public DropStudentCommand(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может отчислять студентов из групп");
        }
        return groupService.dropStudent(request.getData());
    }
}
