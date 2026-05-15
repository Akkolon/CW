package com.bazylev.server.commands.teacher;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.TeacherService;

public class DeleteTeacherCommand implements Command {

    private final TeacherService teacherService;

    public DeleteTeacherCommand(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может удалять преподавателей");
        }
        return teacherService.deleteTeacher(request.getData());
    }
}