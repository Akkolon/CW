package com.bazylev.server.reports;

public abstract class Report {

    protected final String title;

    protected Report(String title) {
        this.title = title;
    }

    public abstract byte[] exportToPdf();

    public abstract String exportToCsv();

    public String getTitle() {
        return title;
    }
}