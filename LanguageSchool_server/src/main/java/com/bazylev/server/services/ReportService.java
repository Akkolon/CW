package com.bazylev.server.services;

import com.bazylev.server.dao.GradeDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.enums.ReportType;
import com.bazylev.server.models.entities.Grade;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Session;
import com.bazylev.server.reports.PdfFontHelper;
import com.bazylev.server.reports.Report;
import com.bazylev.server.reports.ReportFactory;
import com.bazylev.server.reports.attendance.AttendanceReportFactory;
import com.bazylev.server.reports.financial.FinancialReportFactory;
import com.bazylev.server.reports.grade.GradeReportFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.OptionalDouble;

public class ReportService {

    private final GradeDAO        gradeDAO        = new GradeDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final StudentDAO      studentDAO      = new StudentDAO();
    private final PersonDAO       personDAO       = new PersonDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response generateReport(String data, Session session) {
        if (data == null || data.isBlank()) {
            return Response.error("Параметры отчёта не переданы");
        }
        JsonObject params;
        try {
            params = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String typeStr = params.has("reportType") ? params.get("reportType").getAsString() : "";
        String format  = params.has("format") ? params.get("format").getAsString().toUpperCase() : "CSV";

        ReportType reportType;
        try {
            reportType = ReportType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.error("Неизвестный тип отчёта: " + typeStr);
        }

        ReportFactory factory = resolveFactory(reportType);
        if (factory == null) {
            return Response.error("Фабрика для типа отчёта не найдена: " + reportType);
        }

        Report report;
        try {
            report = factory.create(params);
        } catch (Exception e) {
            return Response.error("Ошибка формирования отчёта: " + e.getMessage());
        }

        JsonObject result = new JsonObject();
        result.addProperty("title",  report.getTitle());
        result.addProperty("type",   reportType.name());
        result.addProperty("format", format);

        if ("PDF".equals(format)) {
            byte[] pdfBytes = report.exportToPdf();
            result.addProperty("content", Base64.getEncoder().encodeToString(pdfBytes));
            result.addProperty("contentType", "application/pdf");
        } else {
            result.addProperty("content", report.exportToCsv());
            result.addProperty("contentType", "text/csv");
        }

        return Response.ok(result.toString());
    }

    public Response generateCertificate(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Параметры сертификата не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int studentId = json.has("studentId") ? json.get("studentId").getAsInt() : -1;
        int groupId   = json.has("groupId")   ? json.get("groupId").getAsInt()   : -1;

        if (studentId < 0 || groupId < 0) {
            return Response.error("studentId и groupId обязательны");
        }

        GroupStudent gs = groupStudentDAO
                .findActiveByGroupAndStudent(groupId, studentId)
                .orElse(null);
        if (gs == null) {
            return Response.error("Зачисление студента в данную группу не найдено");
        }

        List<Grade> grades = gradeDAO.findByGroupStudentId(gs.getId());
        OptionalDouble avg  = grades.stream().mapToDouble(Grade::getValue).average();

        if (avg.isEmpty() || avg.getAsDouble() < 6.0) {
            return Response.error("Студент не выполнил требования для получения сертификата " +
                    "(средний балл: " + String.format("%.2f", avg.orElse(0)) + ", необходимо ≥ 6.0)");
        }

        Student student = studentDAO.findById(studentId).orElse(null);
        Person  person  = student != null
                ? personDAO.findById(student.getPersonId()).orElse(null) : null;
        String fullName = person != null ? safeFullName(person) : "Студент #" + studentId;

        byte[] pdfBytes = buildCertificatePdf(fullName, avg.getAsDouble(), groupId);

        JsonObject result = new JsonObject();
        result.addProperty("title",       "Сертификат об окончании курса");
        result.addProperty("studentName", fullName);
        result.addProperty("average",     avg.getAsDouble());
        result.addProperty("content",     Base64.getEncoder().encodeToString(pdfBytes));
        result.addProperty("contentType", "application/pdf");

        return Response.ok(result.toString());
    }

    private ReportFactory resolveFactory(ReportType type) {
        return switch (type) {
            case ATTENDANCE -> new AttendanceReportFactory();
            case GRADES     -> new GradeReportFactory();
            case FINANCIAL  -> new FinancialReportFactory();
            default         -> null;
        };
    }

    private byte[] buildCertificatePdf(String fullName, double average, int groupId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(pdfWriter);
            Document document = new Document(pdf);

            PdfFont fontBold   = PdfFontHelper.loadBold();
            PdfFont fontNormal = PdfFontHelper.loadNormal();

            document.add(new Paragraph("СЕРТИФИКАТ ОБ ОКОНЧАНИИ КУРСА")
                    .setFont(fontBold).setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            document.add(new Paragraph("Настоящим подтверждается, что")
                    .setFont(fontNormal).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(fullName)
                    .setFont(fontBold).setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("успешно завершил(а) обучение (группа #" + groupId + ")")
                    .setFont(fontNormal).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Средний балл: " + String.format("%.2f", average))
                    .setFont(fontBold).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            document.add(new Paragraph("Дата выдачи: " + LocalDate.now())
                    .setFont(fontNormal).setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации PDF сертификата", e);
        }
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   != null ? p.getLastName().strip()   : "";
        String fn = p.getFirstName()  != null ? p.getFirstName().strip()  : "";
        String mn = p.getMiddleName() != null ? p.getMiddleName().strip() : "";
        return (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
    }
}
