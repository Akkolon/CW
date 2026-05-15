package com.bazylev.server.commands.grade;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.GradeService;

public class SetGradeCommand implements Command {

    private final GradeService gradeService;

    public SetGradeCommand(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER)) {
            return Response.forbidden("Только преподаватель или администратор может выставлять оценки");
        }
        return gradeService.setGrade(request.getData());
    }
}
