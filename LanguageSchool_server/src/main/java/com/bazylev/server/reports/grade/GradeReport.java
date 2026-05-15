package com.bazylev.server.reports.grade;

import com.bazylev.server.models.entities.Grade;
import com.bazylev.server.reports.PdfFontHelper;
import com.bazylev.server.reports.Report;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public class GradeReport extends Report {

    private final List<Grade>          grades;
    private final Map<Integer, String> nameByGsId;

    public GradeReport(String title, List<Grade> grades, Map<Integer, String> nameByGsId) {
        super(title);
        this.grades     = grades;
        this.nameByGsId = nameByGsId;
    }

    @Override
    public byte[] exportToPdf() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(pdfWriter);
            Document document = new Document(pdf);

            PdfFont font     = PdfFontHelper.loadNormal();
            PdfFont fontBold = PdfFontHelper.loadBold();

            document.add(new Paragraph(title).setFont(fontBold).setFontSize(16));
            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}))
                    .useAllAvailableWidth();

            addHeaderCell(table, "Студент",    fontBold);
            addHeaderCell(table, "Дата",       fontBold);
            addHeaderCell(table, "Тип",        fontBold);
            addHeaderCell(table, "Оценка",     fontBold);

            for (Grade g : grades) {
                String name = nameByGsId.getOrDefault(g.getGroupStudentId(),
                        "ID " + g.getGroupStudentId());
                table.addCell(new Cell().add(new Paragraph(name).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        g.getGradeDate() != null ? g.getGradeDate().toString() : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        g.getGradeType() != null ? formatType(g.getGradeType().name()) : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        String.valueOf(g.getValue())).setFont(font)));
            }

            document.add(table);

            OptionalDouble avg = grades.stream().mapToDouble(Grade::getValue).average();
            String avgStr = avg.isPresent() ? String.format("%.2f", avg.getAsDouble()) : "—";
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Средний балл: " + avgStr).setFont(fontBold).setFontSize(12));

            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации PDF отчёта успеваемости", e);
        }
    }

    @Override
    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("Студент,Дата,Тип,Оценка\n");
        for (Grade g : grades) {
            String name = nameByGsId.getOrDefault(g.getGroupStudentId(),
                    "ID " + g.getGroupStudentId());
            sb.append(escapeCsv(name)).append(",")
              .append(g.getGradeDate()).append(",")
              .append(g.getGradeType()).append(",")
              .append(g.getValue()).append("\n");
        }

        OptionalDouble avg = grades.stream().mapToDouble(Grade::getValue).average();
        sb.append("\nСредний балл:,").append(avg.isPresent()
                ? String.format("%.2f", avg.getAsDouble()) : "—").append("\n");

        return sb.toString();
    }

    public List<Grade> getGrades() {
        return grades;
    }

    private static String formatType(String type) {
        return switch (type) {
            case "HOMEWORK" -> "Домашнее задание";
            case "TEST"     -> "Тест";
            case "EXAM"     -> "Экзамен";
            case "ACTIVITY" -> "Активность";
            default         -> type;
        };
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void addHeaderCell(Table table, String text, PdfFont font) {
        table.addHeaderCell(new Cell().add(new Paragraph(text).setFont(font)));
    }
}
