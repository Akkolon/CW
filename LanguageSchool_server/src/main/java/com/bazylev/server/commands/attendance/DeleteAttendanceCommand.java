package com.bazylev.server.commands.attendance;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.AttendanceService;

public class DeleteAttendanceCommand implements Command {

    private final AttendanceService attendanceService;

    public DeleteAttendanceCommand(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER)) {
            return Response.forbidden(
                    "Только преподаватель или администратор может удалять записи посещаемости");
        }
        return attendanceService.deleteAttendance(request.getData());
    }
}