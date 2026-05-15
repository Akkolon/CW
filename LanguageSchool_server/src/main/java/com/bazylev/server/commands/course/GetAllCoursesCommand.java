package com.bazylev.server.commands.course;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.CourseService;

public class GetAllCoursesCommand implements Command {

    private final CourseService courseService;

    public GetAllCoursesCommand(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return courseService.getAllCourses();
    }
}
