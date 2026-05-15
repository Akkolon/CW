package com.bazylev.server.commands.teacher;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.TeacherService;

public class GetAllTeachersCommand implements Command {

    private final TeacherService teacherService;

    public GetAllTeachersCommand(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return teacherService.getAllTeachers();
    }
}
