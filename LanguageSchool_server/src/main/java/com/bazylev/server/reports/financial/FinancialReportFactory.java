package com.bazylev.server.reports.financial;

import com.bazylev.server.dao.PaymentDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.models.entities.Payment;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.reports.Report;
import com.bazylev.server.reports.ReportFactory;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialReportFactory extends ReportFactory {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final PersonDAO  personDAO  = new PersonDAO();

    @Override
    protected Report buildReport(JsonObject params) {
        LocalDate from = LocalDate.parse(params.get("from").getAsString());
        LocalDate to   = LocalDate.parse(params.get("to").getAsString());

        List<Payment> payments = paymentDAO.findByDateRange(from, to);

        Map<Integer, String> nameByStudentId = new HashMap<>();
        for (Payment p : payments) {
            if (nameByStudentId.containsKey(p.getStudentId())) continue;
            Student student = studentDAO.findById(p.getStudentId()).orElse(null);
            if (student != null) {
                Person person = personDAO.findById(student.getPersonId()).orElse(null);
                if (person != null) {
                    nameByStudentId.put(p.getStudentId(), safeFullName(person));
                }
            }
        }

        return new FinancialReport("Финансовый отчёт", payments, nameByStudentId);
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   != null ? p.getLastName().strip()   : "";
        String fn = p.getFirstName()  != null ? p.getFirstName().strip()  : "";
        String mn = p.getMiddleName() != null ? p.getMiddleName().strip() : "";
        return (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
    }
}
