package com.bazylev.server.commands.schedule;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ScheduleService;

public class GetScheduleCommand implements Command {

    private final ScheduleService scheduleService;

    public GetScheduleCommand(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER, Role.STUDENT)) {
            return Response.forbidden("Недостаточно прав");
        }
        if (request.getData() != null && request.getData().contains("studentId")) {
            return scheduleService.getScheduleForStudent(request.getData());
        }
        return scheduleService.getSchedule(request.getData(), session);
    }
}
