package com.bazylev.server.services;

import com.bazylev.server.dao.AttendanceDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.enums.AttendanceStatus;
import com.bazylev.server.models.entities.Attendance;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Session;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceService {

    private final AttendanceDAO   attendanceDAO   = new AttendanceDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response markAttendance(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные посещаемости не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        if (!json.has("records") || !json.get("records").isJsonArray()) {
            return Response.error("Поле 'records' обязательно и должно быть массивом");
        }

        JsonArray records = json.getAsJsonArray("records");
        List<Attendance> saved = new ArrayList<>();

        for (JsonElement el : records) {
            JsonObject rec = el.getAsJsonObject();

            int groupStudentId = rec.has("groupStudentId")
                    ? rec.get("groupStudentId").getAsInt() : -1;
            String statusStr   = rec.has("status")
                    ? rec.get("status").getAsString() : "";
            String dateStr     = rec.has("lessonDate")
                    ? rec.get("lessonDate").getAsString() : "";
            String comment     = rec.has("comment")
                    ? rec.get("comment").getAsString() : null;

            if (groupStudentId < 0 || statusStr.isBlank() || dateStr.isBlank()) continue;

            AttendanceStatus status;
            try {
                status = AttendanceStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            LocalDate date = LocalDate.parse(dateStr);

            Optional<Attendance> existing =
                    attendanceDAO.findByGroupStudentAndDate(groupStudentId, date);

            if (existing.isPresent()) {
                Attendance att = existing.get();
                att.setStatus(status);
                att.setComment(comment);
                attendanceDAO.update(att);
                saved.add(att);
            } else {
                Attendance att = new Attendance(0, groupStudentId, date, status, comment);
                int attId = attendanceDAO.save(att);
                att.setId(attId);
                saved.add(att);
            }
        }

        return Response.ok(gson.toJson(saved));
    }

    public Response getAttendance(String data, Session session) {
        if (data == null || data.isBlank()) {
            return Response.error("Параметры запроса не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        if (json.has("groupStudentId")) {
            int gsId = json.get("groupStudentId").getAsInt();
            return Response.ok(gson.toJson(attendanceDAO.findByGroupStudentId(gsId)));
        }

        if (json.has("studentId")) {
            int studentId = json.get("studentId").getAsInt();
            List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(studentId);
            List<Attendance> result = new java.util.ArrayList<>();
            for (GroupStudent gs : enrollments) {
                result.addAll(attendanceDAO.findByGroupStudentId(gs.getId()));
            }
            return Response.ok(gson.toJson(result));
        }

        if (json.has("groupId") && json.has("from") && json.has("to")) {
            int groupId    = json.get("groupId").getAsInt();
            LocalDate from = LocalDate.parse(json.get("from").getAsString());
            LocalDate to   = LocalDate.parse(json.get("to").getAsString());
            return Response.ok(gson.toJson(
                    attendanceDAO.findByGroupIdAndDateRange(groupId, from, to)));
        }

        return Response.error("Укажите studentId, groupStudentId или groupId+from+to");
    }

    public Response updateAttendance(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные посещаемости не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID записи");

        Attendance att = attendanceDAO.findById(id).orElse(null);
        if (att == null) return Response.error("Запись посещаемости не найдена: " + id);

        if (json.has("status")) {
            try {
                att.setStatus(AttendanceStatus.valueOf(
                        json.get("status").getAsString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный статус");
            }
        }
        if (json.has("comment")) {
            att.setComment(json.get("comment").getAsString());
        }
        if (json.has("lessonDate")) {
            att.setLessonDate(
                    java.time.LocalDate.parse(json.get("lessonDate").getAsString()));
        }

        attendanceDAO.update(att);
        return Response.ok(gson.toJson(att));
    }

    public Response deleteAttendance(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID записи не передан");
        }
        int id;
        try {
            id = JsonParser.parseString(data).getAsJsonObject().get("id").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID записи");
        }

        if (attendanceDAO.findById(id).isEmpty()) {
            return Response.error("Запись посещаемости не найдена: " + id);
        }
        attendanceDAO.delete(id);
        return Response.ok();
    }
}