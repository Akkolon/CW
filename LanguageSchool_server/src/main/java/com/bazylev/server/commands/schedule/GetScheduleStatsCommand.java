package com.bazylev.server.commands.schedule;

import com.bazylev.server.commands.Command;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ScheduleService;

public class GetScheduleStatsCommand implements Command {

    private final ScheduleService scheduleService;

    public GetScheduleStatsCommand(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public Response execute(Request request, Session session) {
        return scheduleService.getScheduleStats();
    }
}