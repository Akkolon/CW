package com.bazylev.server.reports.attendance;

import com.bazylev.server.models.entities.Attendance;
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

public class AttendanceReport extends Report {

    private final List<Attendance>    records;
    private final Map<Integer, String> nameByGsId;

    public AttendanceReport(String title, List<Attendance> records,
                            Map<Integer, String> nameByGsId) {
        super(title);
        this.records    = records;
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

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 3}))
                    .useAllAvailableWidth();

            addHeaderCell(table, "Студент",     fontBold);
            addHeaderCell(table, "Дата",        fontBold);
            addHeaderCell(table, "Статус",      fontBold);
            addHeaderCell(table, "Комментарий", fontBold);

            for (Attendance a : records) {
                String name = nameByGsId.getOrDefault(a.getGroupStudentId(),
                        "ID " + a.getGroupStudentId());
                table.addCell(new Cell().add(new Paragraph(name).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        a.getLessonDate() != null ? a.getLessonDate().toString() : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        a.getStatus() != null ? formatStatus(a.getStatus().name()) : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        a.getComment() != null ? a.getComment() : "").setFont(font)));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации PDF отчёта посещаемости", e);
        }
    }

    @Override
    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("Студент,Дата занятия,Статус,Комментарий\n");
        for (Attendance a : records) {
            String name = nameByGsId.getOrDefault(a.getGroupStudentId(),
                    "ID " + a.getGroupStudentId());
            sb.append(escapeCsv(name)).append(",")
              .append(a.getLessonDate()).append(",")
              .append(a.getStatus()).append(",")
              .append(a.getComment() != null ? escapeCsv(a.getComment()) : "").append("\n");
        }
        return sb.toString();
    }

    public List<Attendance> getRecords() {
        return records;
    }

    private static String formatStatus(String status) {
        return switch (status) {
            case "PRESENT" -> "Присутствовал";
            case "LATE"    -> "Опоздал";
            case "ABSENT"  -> "Отсутствовал";
            case "EXCUSED" -> "По уважительной";
            default        -> status;
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
