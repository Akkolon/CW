package com.bazylev.server.commands.student;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.StudentService;

public class GetStudentsByGroupCommand implements Command {

    private final StudentService studentService;

    public GetStudentsByGroupCommand(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER)) {
            return Response.forbidden("Недостаточно прав");
        }
        return studentService.getStudentsByGroup(request.getData());
    }
}