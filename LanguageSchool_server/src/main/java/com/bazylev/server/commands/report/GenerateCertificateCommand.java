package com.bazylev.server.commands.report;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.ReportService;

public class GenerateCertificateCommand implements Command {

    private final ReportService reportService;

    public GenerateCertificateCommand(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может генерировать сертификаты");
        }
        return reportService.generateCertificate(request.getData());
    }
}
