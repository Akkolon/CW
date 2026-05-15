package com.bazylev.server.services;

import com.bazylev.server.dao.AttendanceDAO;
import com.bazylev.server.dao.GradeDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.PaymentDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.dao.TeacherDAO;
import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.entities.Teacher;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.List;

public class UserService {

    private final UserDAO         userDAO         = new UserDAO();
    private final PersonDAO       personDAO       = new PersonDAO();
    private final StudentDAO      studentDAO      = new StudentDAO();
    private final TeacherDAO      teacherDAO      = new TeacherDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final AttendanceDAO   attendanceDAO   = new AttendanceDAO();
    private final GradeDAO        gradeDAO        = new GradeDAO();
    private final PaymentDAO      paymentDAO      = new PaymentDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response getAllUsers() {
        return Response.ok(gson.toJson(userDAO.findAll()));
    }

    public Response createUser(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные пользователя не переданы");
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String login    = getString(json, "login");
        String password = getString(json, "password");
        String roleStr  = getString(json, "role");

        if (login.isBlank() || password.isBlank() || roleStr.isBlank()) {
            return Response.error("Логин, пароль и роль обязательны");
        }

        if (userDAO.findByLogin(login).isPresent()) {
            return Response.error("Пользователь с логином «" + login + "» уже существует");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.error("Неизвестная роль: " + roleStr);
        }

        Person person = new Person();
        person.setFirstName(getString(json, "firstName"));
        person.setLastName(getString(json, "lastName"));
        person.setMiddleName(getString(json, "middleName"));
        person.setEmail(getString(json, "email"));
        int personId = personDAO.save(person);

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(AuthService.hashPassword(password));
        user.setRole(role);
        user.setBlocked(false);
        user.setPersonId(personId);
        int userId = userDAO.save(user);
        user.setId(userId);

        personDAO.setUserId(personId, userId);

        JsonObject result = new JsonObject();
        result.addProperty("id",       userId);
        result.addProperty("personId", personId);
        result.addProperty("login",    login);
        result.addProperty("role",     role.name());
        return Response.ok(result.toString());
    }

    public Response updateUser(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные пользователя не переданы");
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int userId = json.has("id") ? json.get("id").getAsInt() : -1;
        if (userId < 0) {
            return Response.error("Не указан ID пользователя");
        }

        User user = userDAO.findById(userId).orElse(null);
        if (user == null) {
            return Response.error("Пользователь не найден: " + userId);
        }

        if (json.has("role")) {
            Role newRole;
            try {
                newRole = Role.valueOf(json.get("role").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестная роль: " + json.get("role").getAsString());
            }

            if (newRole != user.getRole()) {
                String syncError = syncRoleChange(user.getPersonId(), user.getRole(), newRole);
                if (syncError != null) {
                    return Response.error(syncError);
                }
                user.setRole(newRole);
            }
        }

        if (json.has("password")) {
            String newPassword = json.get("password").getAsString();
            if (!newPassword.isBlank()) {
                user.setPasswordHash(AuthService.hashPassword(newPassword));
            }
        }

        userDAO.update(user);

        boolean hasPersonFields = json.has("firstName") || json.has("lastName")
                || json.has("middleName") || json.has("email");
        if (hasPersonFields) {
            Person person = personDAO.findById(user.getPersonId()).orElse(null);
            if (person != null) {
                if (json.has("firstName") && !json.get("firstName").getAsString().isBlank()) {
                    person.setFirstName(json.get("firstName").getAsString());
                }
                if (json.has("lastName") && !json.get("lastName").getAsString().isBlank()) {
                    person.setLastName(json.get("lastName").getAsString());
                }
                if (json.has("middleName")) {
                    person.setMiddleName(json.get("middleName").getAsString());
                }
                if (json.has("email")) {
                    person.setEmail(json.get("email").getAsString());
                }
                personDAO.update(person);
            }
        }

        return Response.ok(gson.toJson(user));
    }

    private String syncRoleChange(int personId, Role oldRole, Role newRole) {
        try {
            if (oldRole == Role.STUDENT) {
                Student student = studentDAO.findAll().stream()
                        .filter(s -> s.getPersonId() == personId)
                        .findFirst()
                        .orElse(null);
                if (student != null) {
                    removeStudentSafely(student);
                }
            } else if (oldRole == Role.TEACHER) {
                teacherDAO.findAll().stream()
                        .filter(t -> t.getPersonId() == personId)
                        .findFirst()
                        .ifPresent(t -> teacherDAO.delete(t.getId()));
            }

            if (newRole == Role.STUDENT) {
                boolean exists = studentDAO.findAll().stream()
                        .anyMatch(s -> s.getPersonId() == personId);
                if (!exists) {
                    Student student = new Student();
                    student.setPersonId(personId);
                    student.setEnrollmentDate(LocalDate.now());
                    student.setActive(true);
                    studentDAO.save(student);
                }
            } else if (newRole == Role.TEACHER) {
                boolean exists = teacherDAO.findAll().stream()
                        .anyMatch(t -> t.getPersonId() == personId);
                if (!exists) {
                    Teacher teacher = new Teacher();
                    teacher.setPersonId(personId);
                    teacher.setSpecialization("");
                    teacher.setHireDate(LocalDate.now());
                    teacherDAO.save(teacher);
                }
            }

            return null;
        } catch (Exception e) {
            return "Ошибка смены роли: " + e.getMessage();
        }
    }

    private void removeStudentSafely(Student student) {
        List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(student.getId());
        for (GroupStudent gs : enrollments) {
            attendanceDAO.findByGroupStudentId(gs.getId())
                    .forEach(a -> attendanceDAO.delete(a.getId()));
            gradeDAO.findByGroupStudentId(gs.getId())
                    .forEach(g -> gradeDAO.delete(g.getId()));
            groupStudentDAO.delete(gs.getId());
        }
        paymentDAO.findByStudentId(student.getId())
                .forEach(p -> paymentDAO.delete(p.getId()));
        studentDAO.delete(student.getId());
    }

    public Response deleteUser(String data) {
        int userId = parseId(data);
        if (userId < 0) return Response.error("Некорректный ID пользователя");

        User user = userDAO.findById(userId).orElse(null);
        if (user == null) return Response.error("Пользователь не найден: " + userId);

        int personId = user.getPersonId();

        if (user.getRole() == Role.STUDENT) {
            Student student = studentDAO.findAll().stream()
                    .filter(s -> s.getPersonId() == personId)
                    .findFirst()
                    .orElse(null);
            if (student != null) {
                removeStudentSafely(student);
            }
        }

        if (user.getRole() == Role.TEACHER) {
            teacherDAO.findAll().stream()
                    .filter(t -> t.getPersonId() == personId)
                    .findFirst()
                    .ifPresent(t -> teacherDAO.delete(t.getId()));
        }

        userDAO.delete(userId);
        return Response.ok();
    }

    public Response setBlocked(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int userId      = json.has("userId")  ? json.get("userId").getAsInt()   : -1;
        boolean blocked = json.has("blocked") && json.get("blocked").getAsBoolean();

        if (userId < 0) return Response.error("Не указан ID пользователя");

        userDAO.setBlocked(userId, blocked);
        return Response.ok();
    }

    public Response getActionLog(String data) {
        return Response.ok("[]");
    }

    public Response getAllPersons() {
        return Response.ok(gson.toJson(personDAO.findAll()));
    }

    private String getString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private int parseId(String data) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            return json.has("id") ? json.get("id").getAsInt() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
