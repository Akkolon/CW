package com.bazylev.server.commands.grade;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.GradeService;

public class GetAverageGradeCommand implements Command {

    private final GradeService gradeService;

    public GetAverageGradeCommand(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return gradeService.getAverageGrade(request.getData(), session);
    }
}
