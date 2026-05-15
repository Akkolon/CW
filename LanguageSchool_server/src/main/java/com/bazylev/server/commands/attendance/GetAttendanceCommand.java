package com.bazylev.server.commands.attendance;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.AttendanceService;

public class GetAttendanceCommand implements Command {

    private final AttendanceService attendanceService;

    public GetAttendanceCommand(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER, Role.STUDENT)) {
            return Response.forbidden("Недостаточно прав");
        }
        return attendanceService.getAttendance(request.getData(), session);
    }
}
