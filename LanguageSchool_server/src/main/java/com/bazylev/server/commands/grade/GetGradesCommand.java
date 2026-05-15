package com.bazylev.server.commands.grade;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.GradeService;

public class GetGradesCommand implements Command {

    private final GradeService gradeService;

    public GetGradesCommand(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER, Role.STUDENT)) {
            return Response.forbidden("Недостаточно прав");
        }
        return gradeService.getGrades(request.getData(), session);
    }
}
