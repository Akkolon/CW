package com.bazylev.server.commands.teacher;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.TeacherService;

public class UpdateTeacherCommand implements Command {

    private final TeacherService teacherService;

    public UpdateTeacherCommand(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может изменять данные преподавателей");
        }
        return teacherService.updateTeacher(request.getData());
    }
}
