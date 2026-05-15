package com.bazylev.server.services;

import com.bazylev.server.dao.GroupDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.ScheduleDAO;
import com.bazylev.server.dao.TeacherDAO;
import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Teacher;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.List;

public class TeacherService {

    private final TeacherDAO  teacherDAO  = new TeacherDAO();
    private final PersonDAO   personDAO   = new PersonDAO();
    private final UserDAO     userDAO     = new UserDAO();
    private final GroupDAO    groupDAO    = new GroupDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final Gson        gson        = GsonFactory.getInstance();

    public Response getAllTeachers() {
        return Response.ok(gson.toJson(teacherDAO.findAll()));
    }

    public Response createTeacher(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные преподавателя не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String firstName  = json.has("firstName")  ? json.get("firstName").getAsString()  : "";
        String lastName   = json.has("lastName")   ? json.get("lastName").getAsString()   : "";
        String middleName = json.has("middleName") ? json.get("middleName").getAsString() : "";
        String email      = json.has("email")      ? json.get("email").getAsString()      : "";
        String login      = json.has("login")      ? json.get("login").getAsString()      : "";
        String password   = json.has("password")   ? json.get("password").getAsString()   : "";

        if (firstName.isBlank() || lastName.isBlank()) {
            return Response.error("Имя и фамилия преподавателя обязательны");
        }

        if (json.has("personId")) {
            int personId = json.get("personId").getAsInt();
            Person person = personDAO.findById(personId).orElse(null);
            if (person == null) {
                return Response.error("Персона не найдена: " + personId);
            }
            Teacher teacher = new Teacher();
            teacher.setPersonId(personId);
            teacher.setSpecialization(
                    json.has("specialization") ? json.get("specialization").getAsString() : "");
            teacher.setHireDate(LocalDate.now());
            int teacherId = teacherDAO.save(teacher);
            teacher.setId(teacherId);
            return Response.ok(gson.toJson(teacher));
        }

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setMiddleName(middleName.isBlank() ? null : middleName);
        person.setEmail(email.isBlank() ? null : email);
        int personId = personDAO.save(person);

        if (!login.isBlank() && !password.isBlank()) {
            if (userDAO.findByLogin(login).isPresent()) {
                personDAO.delete(personId);
                return Response.error("Пользователь с логином «" + login + "» уже существует");
            }
            User user = new User();
            user.setLogin(login);
            user.setPasswordHash(AuthService.hashPassword(password));
            user.setRole(Role.TEACHER);
            user.setBlocked(false);
            user.setPersonId(personId);
            int userId = userDAO.save(user);
            personDAO.setUserId(personId, userId);
        }

        Teacher teacher = new Teacher();
        teacher.setPersonId(personId);
        teacher.setSpecialization(
                json.has("specialization") ? json.get("specialization").getAsString() : "");
        teacher.setHireDate(LocalDate.now());
        int teacherId = teacherDAO.save(teacher);
        teacher.setId(teacherId);
        return Response.ok(gson.toJson(teacher));
    }

    public Response updateTeacher(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные преподавателя не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID преподавателя");

        Teacher teacher = teacherDAO.findById(id).orElse(null);
        if (teacher == null) return Response.error("Преподаватель не найден: " + id);

        if (json.has("specialization")) {
            teacher.setSpecialization(json.get("specialization").getAsString());
        }
        teacherDAO.update(teacher);

        Person person = personDAO.findById(teacher.getPersonId()).orElse(null);
        if (person != null) {
            if (json.has("firstName") && !json.get("firstName").getAsString().isBlank())
                person.setFirstName(json.get("firstName").getAsString());
            if (json.has("lastName") && !json.get("lastName").getAsString().isBlank())
                person.setLastName(json.get("lastName").getAsString());
            if (json.has("middleName"))
                person.setMiddleName(json.get("middleName").getAsString());
            if (json.has("email"))
                person.setEmail(json.get("email").getAsString());
            personDAO.update(person);

            if (json.has("password") && !json.get("password").getAsString().isBlank()) {
                if (person.getUserId() != null) {
                    userDAO.findById(person.getUserId()).ifPresent(user -> {
                        user.setPasswordHash(
                                AuthService.hashPassword(json.get("password").getAsString()));
                        userDAO.update(user);
                    });
                }
            }
        }

        return Response.ok(gson.toJson(teacher));
    }

    public Response deleteTeacher(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID преподавателя не передан");
        }
        int teacherId;
        try {
            teacherId = JsonParser.parseString(data)
                    .getAsJsonObject().get("teacherId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID преподавателя");
        }

        Teacher teacher = teacherDAO.findById(teacherId).orElse(null);
        if (teacher == null) return Response.error("Преподаватель не найден: " + teacherId);

        groupDAO.findByTeacherId(teacherId).forEach(group -> {
            group.setTeacherId(0);
            groupDAO.update(group);
        });

        groupDAO.findByTeacherId(teacherId).forEach(group ->
                scheduleDAO.findByGroupId(group.getId())
                        .forEach(slot -> scheduleDAO.delete(slot.getId())));

        int personId = teacher.getPersonId();
        teacherDAO.delete(teacherId);

        Person person = personDAO.findById(personId).orElse(null);
        if (person != null) {
            if (person.getUserId() != null) {
                userDAO.delete(person.getUserId());
            }
            personDAO.delete(personId);
        }

        return Response.ok();
    }
}
