package com.bazylev.server.services;

import com.bazylev.server.dao.CourseDAO;
import com.bazylev.server.dao.GroupDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.db.ConnectionPool;
import com.bazylev.server.enums.EnrollmentStatus;
import com.bazylev.server.enums.GroupStatus;
import com.bazylev.server.models.entities.*;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class GroupService {

    private final GroupDAO        groupDAO        = new GroupDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final StudentDAO      studentDAO      = new StudentDAO();
    private final ConnectionPool  pool            = ConnectionPool.getInstance();
    private final Gson            gson            = GsonFactory.getInstance();
    private final com.bazylev.server.dao.UserDAO    userDAO    = new com.bazylev.server.dao.UserDAO();
    private final com.bazylev.server.dao.TeacherDAO teacherDAO = new com.bazylev.server.dao.TeacherDAO();

    public Response getAllGroups(Session session) {
        List<Group> groups;
        if (session.isTeacher()) {
            com.bazylev.server.models.entities.User user =
                    userDAO.findById(session.getUserId()).orElse(null);
            if (user == null) {
                return Response.error("Пользователь не найден");
            }
            Teacher teacher = teacherDAO.findAll().stream()
                    .filter(t -> t.getPersonId() == user.getPersonId())
                    .findFirst()
                    .orElse(null);
            if (teacher == null) {
               groups = java.util.List.of();
            } else {
                groups = groupDAO.findByTeacherId(teacher.getId());
            }
        } else {
            groups = groupDAO.findAll();
        }

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (Group g : groups) {
            com.google.gson.JsonObject obj = gson.toJsonTree(g).getAsJsonObject();
            obj.addProperty("studentCount", groupDAO.countActiveStudents(g.getId()));
            arr.add(obj);
        }
        return Response.ok(arr.toString());
    }

    public Response createGroup(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные группы не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        if (!json.has("courseId") || !json.has("teacherId")) {
            return Response.error("courseId и teacherId обязательны");
        }

        Group group = new Group();
        group.setName(json.has("name") ? json.get("name").getAsString() : "");
        group.setCourseId(json.get("courseId").getAsInt());
        group.setTeacherId(json.get("teacherId").getAsInt());
        group.setMaxStudents(json.has("maxStudents") ? json.get("maxStudents").getAsInt() : 15);
        group.setStatus(GroupStatus.IN_PROGRESS);
        group.setStartDate(json.has("startDate")
                ? LocalDate.parse(json.get("startDate").getAsString()) : LocalDate.now());
        group.setEndDate(json.has("endDate")
                ? LocalDate.parse(json.get("endDate").getAsString()) : null);

        int id = groupDAO.save(group);
        group.setId(id);
        return Response.ok(gson.toJson(group));
    }

    public Response updateGroup(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные группы не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID группы");

        Group group = groupDAO.findById(id).orElse(null);
        if (group == null) return Response.error("Группа не найдена: " + id);

        if (json.has("name"))        group.setName(json.get("name").getAsString());
        if (json.has("teacherId"))   group.setTeacherId(json.get("teacherId").getAsInt());
        if (json.has("maxStudents")) group.setMaxStudents(json.get("maxStudents").getAsInt());
        if (json.has("status")) {
            try {
                group.setStatus(GroupStatus.valueOf(json.get("status").getAsString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный статус группы");
            }
        }
        if (json.has("endDate")) {
            group.setEndDate(LocalDate.parse(json.get("endDate").getAsString()));
        }

        groupDAO.update(group);
        return Response.ok(gson.toJson(group));
    }

    public Response enrollStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные зачисления не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int groupId   = json.has("groupId")   ? json.get("groupId").getAsInt()   : -1;
        int studentId = json.has("studentId") ? json.get("studentId").getAsInt() : -1;

        if (groupId < 0 || studentId < 0) {
            return Response.error("groupId и studentId обязательны");
        }

        Group group = groupDAO.findById(groupId).orElse(null);
        if (group == null) {
            return Response.error("Группа не найдена: " + groupId);
        }

        if (group.getStatus() == GroupStatus.COMPLETED) {
            return Response.error("Нельзя зачислить в завершённую группу");
        }

        int activeCount = groupDAO.countActiveStudents(groupId);
        if (activeCount >= group.getMaxStudents()) {
            return Response.error("В группе нет свободных мест (максимум: " + group.getMaxStudents() + ")");
        }

        Student student = studentDAO.findById(studentId).orElse(null);
        if (student == null) {
            return Response.error("Студент не найден: " + studentId);
        }

        Optional<GroupStudent> existing =
                groupStudentDAO.findActiveByGroupAndStudent(groupId, studentId);
        if (existing.isPresent()) {
            return Response.error("Студент уже зачислен в эту группу");
        }

        Connection connection = pool.getConnection();
        try {
            connection.setAutoCommit(false);

            GroupStudent gs = new GroupStudent();
            gs.setGroupId(groupId);
            gs.setStudentId(studentId);
            gs.setEnrollmentDate(LocalDate.now());
            gs.setStatus(EnrollmentStatus.ACTIVE);
            int gsId = groupStudentDAO.save(gs);
            gs.setId(gsId);

            connection.commit();
            return Response.ok(gson.toJson(gs));
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            return Response.error("Ошибка зачисления студента: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            pool.releaseConnection(connection);
        }
    }

    public Response dropStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные отчисления не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int groupId   = json.has("groupId")   ? json.get("groupId").getAsInt()   : -1;
        int studentId = json.has("studentId") ? json.get("studentId").getAsInt() : -1;

        if (groupId < 0 || studentId < 0) {
            return Response.error("groupId и studentId обязательны");
        }

        GroupStudent gs = groupStudentDAO
                .findActiveByGroupAndStudent(groupId, studentId)
                .orElse(null);

        if (gs == null) {
            return Response.error("Активное зачисление не найдено");
        }

        groupStudentDAO.updateStatus(gs.getId(), EnrollmentStatus.DROPPED);
        return Response.ok();
    }
}