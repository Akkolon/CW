package com.bazylev.server.commands.course;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.CourseService;

public class CreateCourseCommand implements Command {

    private final CourseService courseService;

    public CreateCourseCommand(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может создавать курсы");
        }
        return courseService.createCourse(request.getData());
    }
}
