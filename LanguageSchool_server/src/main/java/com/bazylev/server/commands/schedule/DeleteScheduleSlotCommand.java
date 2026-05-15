package com.bazylev.server.commands.schedule;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ScheduleService;

public class DeleteScheduleSlotCommand implements Command {

    private final ScheduleService scheduleService;

    public DeleteScheduleSlotCommand(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может удалять слоты расписания");
        }
        return scheduleService.deleteSlot(request.getData());
    }
}