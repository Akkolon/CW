package com.bazylev.server.commands.report;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ReportService;

public class GenerateReportCommand implements Command {

    private final ReportService reportService;

    public GenerateReportCommand(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.TEACHER)) {
            return Response.forbidden("Недостаточно прав для формирования отчётов");
        }
        return reportService.generateReport(request.getData(), session);
    }
}
