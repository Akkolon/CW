package com.bazylev.server.reports.financial;

import com.bazylev.server.models.entities.Payment;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class FinancialReport extends Report {

    private final List<Payment>        payments;
    private final Map<Integer, String> nameByStudentId;

    public FinancialReport(String title, List<Payment> payments,
                           Map<Integer, String> nameByStudentId) {
        super(title);
        this.payments        = payments;
        this.nameByStudentId = nameByStudentId;
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

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}))
                    .useAllAvailableWidth();

            addHeaderCell(table, "Студент",       fontBold);
            addHeaderCell(table, "Дата",          fontBold);
            addHeaderCell(table, "Сумма",         fontBold);
            addHeaderCell(table, "Способ оплаты", fontBold);
            addHeaderCell(table, "Номер чека",    fontBold);

            BigDecimal total = BigDecimal.ZERO;
            for (Payment p : payments) {
                String name = nameByStudentId.getOrDefault(p.getStudentId(),
                        "ID " + p.getStudentId());
                table.addCell(new Cell().add(new Paragraph(name).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        p.getPaymentDate() != null ? p.getPaymentDate().toString() : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        p.getAmount() != null ? p.getAmount().toPlainString() : "0").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        p.getPaymentMethod() != null
                                ? formatMethod(p.getPaymentMethod().name()) : "").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        p.getReceiptNumber() != null ? p.getReceiptNumber() : "").setFont(font)));
                if (p.getAmount() != null) total = total.add(p.getAmount());
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Итого: " + total.toPlainString() + " руб.")
                    .setFont(fontBold).setFontSize(12));

            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации PDF финансового отчёта", e);
        }
    }

    @Override
    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("Студент,Дата,Сумма,Способ оплаты,Номер чека\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            String name = nameByStudentId.getOrDefault(p.getStudentId(),
                    "ID " + p.getStudentId());
            sb.append(escapeCsv(name)).append(",")
              .append(p.getPaymentDate()).append(",")
              .append(p.getAmount()).append(",")
              .append(p.getPaymentMethod()).append(",")
              .append(p.getReceiptNumber() != null ? escapeCsv(p.getReceiptNumber()) : "")
              .append("\n");
            if (p.getAmount() != null) total = total.add(p.getAmount());
        }
        sb.append("\nИтого:,,").append(total).append("\n");
        return sb.toString();
    }

    public List<Payment> getPayments() {
        return payments;
    }

    private static String formatMethod(String method) {
        return switch (method) {
            case "CASH"     -> "Наличные";
            case "CARD"     -> "Карта";
            case "TRANSFER" -> "Перевод";
            default         -> method;
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
