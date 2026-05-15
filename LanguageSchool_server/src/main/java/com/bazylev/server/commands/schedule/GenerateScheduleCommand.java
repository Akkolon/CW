package com.bazylev.server.commands.schedule;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ScheduleService;

public class GenerateScheduleCommand implements Command {

    private final ScheduleService scheduleService;

    public GenerateScheduleCommand(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может запускать генерацию расписания");
        }
        return scheduleService.generateSchedule(request.getData());
    }
}
