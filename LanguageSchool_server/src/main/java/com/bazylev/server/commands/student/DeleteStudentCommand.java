package com.bazylev.server.commands.student;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.StudentService;

public class DeleteStudentCommand implements Command {

    private final StudentService studentService;

    public DeleteStudentCommand(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может удалять студентов");
        }
        return studentService.deleteStudent(request.getData());
    }
}