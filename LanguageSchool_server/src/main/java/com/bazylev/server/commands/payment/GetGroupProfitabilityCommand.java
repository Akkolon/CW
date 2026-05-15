package com.bazylev.server.commands.payment;

import com.bazylev.server.commands.Command;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.tcp.Request;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.Session;
import com.bazylev.server.services.PaymentService;

public class GetGroupProfitabilityCommand implements Command {

    private final PaymentService paymentService;

    public GetGroupProfitabilityCommand(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Response execute(Request request, Session session) {
        if (!session.hasAnyRole(Role.ADMIN)) {
            return Response.forbidden("Только администратор может просматривать рентабельность групп");
        }
        return paymentService.getGroupProfitability(request.getData());
    }
}
