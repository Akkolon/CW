package com.bazylev.server.reports;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.IOException;
import java.io.InputStream;

public final class PdfFontHelper {

    private PdfFontHelper() {}

    public static PdfFont loadNormal() throws IOException {
        return loadFromClasspath("/fonts/FreeSans.ttf");
    }

    public static PdfFont loadBold() throws IOException {
        return loadFromClasspath("/fonts/FreeSansBold.ttf");
    }

    private static PdfFont loadFromClasspath(String path) throws IOException {
        try (InputStream is = PdfFontHelper.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Шрифт не найден в classpath: " + path);
            }
            byte[] bytes = is.readAllBytes();
            return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H);
        }
    }
}
