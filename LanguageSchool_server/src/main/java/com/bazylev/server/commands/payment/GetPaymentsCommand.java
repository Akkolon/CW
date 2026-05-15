package com.bazylev.server.commands.payment;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.PaymentService;

public class GetPaymentsCommand implements Command {

    private final PaymentService paymentService;

    public GetPaymentsCommand(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN, Role.STUDENT)) {
            return Response.forbidden("Недостаточно прав для просмотра платежей");
        }
        return paymentService.getPayments(request.getData());
    }
}
