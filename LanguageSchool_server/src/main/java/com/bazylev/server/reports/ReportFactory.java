package com.bazylev.server.reports;

import com.google.gson.JsonObject;

public abstract class ReportFactory {

    public final Report create(JsonObject params) {
        Report report = buildReport(params);
        report.exportToCsv();
        return report;
    }

    protected abstract Report buildReport(JsonObject params);
}