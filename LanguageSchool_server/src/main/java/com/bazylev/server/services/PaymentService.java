package com.bazylev.server.services;

import com.bazylev.server.dao.*;
import com.bazylev.server.enums.EnrollmentStatus;
import com.bazylev.server.models.entities.*;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class PaymentService {

    private static final int DEBT_OVERDUE_DAYS = 30;

    private final PaymentDAO      paymentDAO      = new PaymentDAO();
    private final PersonDAO personDAO      = new PersonDAO();
    private final StudentDAO      studentDAO      = new StudentDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final GroupDAO        groupDAO        = new GroupDAO();
    private final CourseDAO       courseDAO       = new CourseDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response registerPayment(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные платежа не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int studentId = json.has("studentId") ? json.get("studentId").getAsInt() : -1;
        if (studentId < 0) return Response.error("studentId обязателен");

        if (studentDAO.findById(studentId).isEmpty()) {
            return Response.error("Студент не найден: " + studentId);
        }

        BigDecimal amount = json.has("amount")
                ? json.get("amount").getAsBigDecimal() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.error("Сумма платежа должна быть положительной");
        }

        com.bazylev.server.enums.PaymentMethod method =
                com.bazylev.server.enums.PaymentMethod.CASH;
        if (json.has("paymentMethod")) {
            try {
                method = com.bazylev.server.enums.PaymentMethod
                        .valueOf(json.get("paymentMethod").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный способ оплаты");
            }
        }

        String dateStr      = json.has("paymentDate") ? json.get("paymentDate").getAsString() : null;
        LocalDate date      = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        String receiptNumber = json.has("receiptNumber") ? json.get("receiptNumber").getAsString() : null;

        Payment payment = new Payment(0, studentId, amount, date, method, receiptNumber);
        int id = paymentDAO.save(payment);
        payment.setId(id);

        return Response.ok(gson.toJson(payment));
    }

    public Response getPayments(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Параметры запроса не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        if (json.has("studentId")) {
            int studentId = json.get("studentId").getAsInt();
            return Response.ok(gson.toJson(paymentDAO.findByStudentId(studentId)));
        }

        if (json.has("from") && json.has("to")) {
            LocalDate from = LocalDate.parse(json.get("from").getAsString());
            LocalDate to   = LocalDate.parse(json.get("to").getAsString());
            return Response.ok(gson.toJson(paymentDAO.findByDateRange(from, to)));
        }

        return Response.error("Укажите studentId или from+to");
    }

    public Response getDebt(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("studentId не передан");
        }
        int studentId;
        try {
            studentId = JsonParser.parseString(data).getAsJsonObject().get("studentId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID студента");
        }

        List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(studentId);
        BigDecimal totalCharged = BigDecimal.ZERO;

        for (GroupStudent gs : enrollments) {
            if (gs.getStatus() != EnrollmentStatus.ACTIVE) continue;
            Group group = groupDAO.findById(gs.getGroupId()).orElse(null);
            if (group == null) continue;
            Course course = courseDAO.findById(group.getCourseId()).orElse(null);
            if (course == null || course.getPricePerMonth() == null) continue;

            LocalDate start = gs.getEnrollmentDate() != null
                    ? gs.getEnrollmentDate() : group.getStartDate();
            long months = start != null
                    ? ChronoUnit.MONTHS.between(start, LocalDate.now()) + 1 : 1;
            totalCharged = totalCharged.add(
                    course.getPricePerMonth().multiply(BigDecimal.valueOf(months)));
        }

        BigDecimal totalPaid = paymentDAO.getTotalPaidByStudent(studentId);
        BigDecimal debt      = totalCharged.subtract(totalPaid);

        JsonObject result = new JsonObject();
        result.addProperty("studentId",    studentId);
        result.addProperty("totalCharged", totalCharged);
        result.addProperty("totalPaid",    totalPaid);
        result.addProperty("debt",         debt.max(BigDecimal.ZERO));

        return Response.ok(result.toString());
    }

    public Response getDebtors() {
        List<Student> students = studentDAO.findAll();
        JsonArray debtors = new JsonArray();

        for (Student student : students) {
            List<GroupStudent> active = groupStudentDAO.findByStudentId(student.getId())
                    .stream()
                    .filter(gs -> gs.getStatus() == EnrollmentStatus.ACTIVE)
                    .toList();

            if (active.isEmpty()) continue;

            for (GroupStudent gs : active) {
                Group group = groupDAO.findById(gs.getGroupId()).orElse(null);
                if (group == null) continue;
                Course course = courseDAO.findById(group.getCourseId()).orElse(null);
                if (course == null || course.getPricePerMonth() == null) continue;

                LocalDate start = gs.getEnrollmentDate() != null
                        ? gs.getEnrollmentDate() : group.getStartDate();
                long months = start != null
                        ? ChronoUnit.MONTHS.between(start, LocalDate.now()) + 1 : 1;
                BigDecimal totalCharged = course.getPricePerMonth()
                        .multiply(BigDecimal.valueOf(months));

                BigDecimal totalPaid = paymentDAO.getTotalPaidByStudent(student.getId());
                BigDecimal debt = totalCharged.subtract(totalPaid);

                if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;

                Person person = personDAO.findById(student.getPersonId()).orElse(null);
                String fullName = person != null ? person.getFullName() : "Студент #" + student.getId();

                JsonObject item = new JsonObject();
                item.addProperty("studentId",  student.getId());
                item.addProperty("fullName",   fullName);
                item.addProperty("courseName", course.getName());
                item.addProperty("debt",       debt);
                debtors.add(item);
            }
        }

        return Response.ok(debtors.toString());
    }

    public Response getGroupProfitability(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("groupId не передан");
        }
        int groupId;
        try {
            groupId = JsonParser.parseString(data).getAsJsonObject().get("groupId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID группы");
        }

        Group group = groupDAO.findById(groupId).orElse(null);
        if (group == null) return Response.error("Группа не найдена: " + groupId);

        List<GroupStudent> enrollments = groupStudentDAO.findByGroupId(groupId);
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (GroupStudent gs : enrollments) {
            totalRevenue = totalRevenue.add(paymentDAO.getTotalPaidByStudent(gs.getStudentId()));
        }

        Course course = courseDAO.findById(group.getCourseId()).orElse(null);
        BigDecimal pricePerMonth = (course != null && course.getPricePerMonth() != null)
                ? course.getPricePerMonth() : BigDecimal.ZERO;

        LocalDate start = group.getStartDate() != null ? group.getStartDate() : LocalDate.now();
        long months     = ChronoUnit.MONTHS.between(start, LocalDate.now()) + 1;

        BigDecimal teacherCost = pricePerMonth
                .multiply(BigDecimal.valueOf(0.3))
                .multiply(BigDecimal.valueOf(months));

        BigDecimal profit = totalRevenue.subtract(teacherCost);
        BigDecimal profitability = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        JsonObject result = new JsonObject();
        result.addProperty("groupId",       groupId);
        result.addProperty("totalRevenue",  totalRevenue);
        result.addProperty("teacherCost",   teacherCost);
        result.addProperty("profit",        profit);
        result.addProperty("profitability", profitability);

        return Response.ok(result.toString());
    }
}