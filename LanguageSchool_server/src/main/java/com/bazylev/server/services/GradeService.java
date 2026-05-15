package com.bazylev.server.services;

import com.bazylev.server.dao.GradeDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.enums.GradeType;
import com.bazylev.server.models.entities.Grade;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

public class GradeService {

    private final GradeDAO        gradeDAO        = new GradeDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response setGrade(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные оценки не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int groupStudentId = json.has("groupStudentId") ? json.get("groupStudentId").getAsInt() : -1;
        if (groupStudentId < 0) return Response.error("groupStudentId обязателен");

        double value = json.has("value") ? json.get("value").getAsDouble() : -1;
        if (value < 0 || value > 10) return Response.error("Оценка должна быть в диапазоне 0–10");

        GradeType type = GradeType.HOMEWORK;
        if (json.has("gradeType")) {
            try {
                type = GradeType.valueOf(json.get("gradeType").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный тип оценки");
            }
        }

        String dateStr  = json.has("gradeDate") ? json.get("gradeDate").getAsString() : null;
        LocalDate date  = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        String comment  = json.has("comment") ? json.get("comment").getAsString() : null;

        Grade grade = new Grade(0, groupStudentId, date, value, type, comment);
        int id = gradeDAO.save(grade);
        grade.setId(id);

        return Response.ok(gson.toJson(grade));
    }

    public Response getGrades(String data, Session session) {
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
            return Response.ok(gson.toJson(gradeDAO.findByGroupStudentId(gsId)));
        }

        if (json.has("studentId")) {
            int studentId = json.get("studentId").getAsInt();
            List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(studentId);
            List<Grade> result = new java.util.ArrayList<>();
            for (GroupStudent gs : enrollments) {
                result.addAll(gradeDAO.findByGroupStudentId(gs.getId()));
            }
            return Response.ok(gson.toJson(result));
        }

        if (json.has("groupId") && json.has("from") && json.has("to")) {
            int groupId    = json.get("groupId").getAsInt();
            LocalDate from = LocalDate.parse(json.get("from").getAsString());
            LocalDate to   = LocalDate.parse(json.get("to").getAsString());
            return Response.ok(gson.toJson(
                    gradeDAO.findByGroupIdAndDateRange(groupId, from, to)));
        }

        return Response.error("Укажите studentId, groupStudentId или groupId+from+to");
    }

    public Response getAverageGrade(String data, Session session) {
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
            OptionalDouble avg = gradeDAO.calcAverageByGroupStudent(gsId);

            JsonObject result = new JsonObject();
            result.addProperty("groupStudentId", gsId);
            result.addProperty("average", avg.isPresent() ? avg.getAsDouble() : 0.0);
            return Response.ok(result.toString());
        }

        if (json.has("groupId")) {
            int groupId = json.get("groupId").getAsInt();
            List<GroupStudent> enrollments = groupStudentDAO.findByGroupId(groupId);

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (GroupStudent gs : enrollments) {
                OptionalDouble avg = gradeDAO.calcAverageByGroupStudent(gs.getId());
                JsonObject item = new JsonObject();
                item.addProperty("groupStudentId", gs.getId());
                item.addProperty("studentId",      gs.getStudentId());
                item.addProperty("average",        avg.isPresent() ? avg.getAsDouble() : 0.0);
                arr.add(item);
            }
            return Response.ok(arr.toString());
        }

        return Response.error("Укажите groupStudentId или groupId");
    }

    public Response updateGrade(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные оценки не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID оценки");

        Grade grade = gradeDAO.findById(id).orElse(null);
        if (grade == null) return Response.error("Оценка не найдена: " + id);

        if (json.has("value")) {
            double value = json.get("value").getAsDouble();
            if (value < 0 || value > 10) {
                return Response.error("Оценка должна быть в диапазоне 0–10");
            }
            grade.setValue(value);
        }
        if (json.has("gradeType")) {
            try {
                grade.setGradeType(GradeType.valueOf(
                        json.get("gradeType").getAsString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный тип оценки");
            }
        }
        if (json.has("gradeDate")) {
            grade.setGradeDate(
                    java.time.LocalDate.parse(json.get("gradeDate").getAsString()));
        }

        gradeDAO.update(grade);
        return Response.ok(gson.toJson(grade));
    }

    public Response deleteGrade(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID оценки не передан");
        }
        int id;
        try {
            id = JsonParser.parseString(data).getAsJsonObject().get("id").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID оценки");
        }

        if (gradeDAO.findById(id).isEmpty()) {
            return Response.error("Оценка не найдена: " + id);
        }
        gradeDAO.delete(id);
        return Response.ok();
    }
}