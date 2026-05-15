package com.bazylev.server.services;

import com.bazylev.server.dao.AttendanceDAO;
import com.bazylev.server.dao.GradeDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.PaymentDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.Attendance;
import com.bazylev.server.models.entities.Grade;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Payment;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    private final StudentDAO      studentDAO      = new StudentDAO();
    private final PersonDAO       personDAO       = new PersonDAO();
    private final UserDAO         userDAO         = new UserDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final AttendanceDAO   attendanceDAO   = new AttendanceDAO();
    private final GradeDAO        gradeDAO        = new GradeDAO();
    private final PaymentDAO      paymentDAO      = new PaymentDAO();
    private final Gson            gson            = GsonFactory.getInstance();

    public Response getAllStudents() {
        return Response.ok(gson.toJson(studentDAO.findAll()));
    }

    public Response createStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные студента не переданы");
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
            return Response.error("Имя и фамилия студента обязательны");
        }

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setMiddleName(middleName);
        person.setEmail(email);
        int personId = personDAO.save(person);

        if (!login.isBlank() && !password.isBlank()) {
            if (userDAO.findByLogin(login).isPresent()) {
                return Response.error("Пользователь с логином «" + login + "» уже существует");
            }
            User user = new User();
            user.setLogin(login);
            user.setPasswordHash(AuthService.hashPassword(password));
            user.setRole(Role.STUDENT);
            user.setBlocked(false);
            user.setPersonId(personId);
            int userId = userDAO.save(user);
            personDAO.setUserId(personId, userId);
        }

        Student student = new Student();
        student.setPersonId(personId);
        student.setEnrollmentDate(LocalDate.now());
        student.setActive(true);

        int studentId = studentDAO.save(student);
        student.setId(studentId);
        return Response.ok(gson.toJson(student));
    }

    public Response registerStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные для регистрации не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String firstName  = json.has("firstName")  ? json.get("firstName").getAsString().strip()  : "";
        String lastName   = json.has("lastName")   ? json.get("lastName").getAsString().strip()   : "";
        String middleName = json.has("middleName") ? json.get("middleName").getAsString().strip() : "";
        String email      = json.has("email")      ? json.get("email").getAsString().strip()      : "";
        String login      = json.has("login")      ? json.get("login").getAsString().strip()      : "";
        String password   = json.has("password")   ? json.get("password").getAsString()           : "";

        if (firstName.isBlank() || lastName.isBlank()) {
            return Response.error("Имя и фамилия обязательны");
        }
        if (login.isBlank() || password.isBlank()) {
            return Response.error("Логин и пароль обязательны");
        }
        if (userDAO.findByLogin(login).isPresent()) {
            return Response.error("Пользователь с логином «" + login + "» уже существует");
        }

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setMiddleName(middleName.isBlank() ? null : middleName);
        person.setEmail(email.isBlank() ? null : email);
        int personId = personDAO.save(person);

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(AuthService.hashPassword(password));
        user.setRole(Role.STUDENT);
        user.setBlocked(false);
        user.setPersonId(personId);
        int userId = userDAO.save(user);
        personDAO.setUserId(personId, userId);

        Student student = new Student();
        student.setPersonId(personId);
        student.setEnrollmentDate(LocalDate.now());
        student.setActive(true);
        int studentId = studentDAO.save(student);
        student.setId(studentId);

        JsonObject result = new JsonObject();
        result.addProperty("studentId", studentId);
        result.addProperty("personId",  personId);
        result.addProperty("userId",    userId);
        return Response.ok(result.toString());
    }

    public Response updateStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные студента не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID студента");

        Student student = studentDAO.findById(id).orElse(null);
        if (student == null) return Response.error("Студент не найден: " + id);

        if (json.has("active")) student.setActive(json.get("active").getAsBoolean());
        studentDAO.update(student);

        Person person = personDAO.findById(student.getPersonId()).orElse(null);
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

        return Response.ok(gson.toJson(student));
    }

    public Response getStudentHistory(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID студента не передан");
        }
        int studentId;
        try {
            studentId = JsonParser.parseString(data).getAsJsonObject().get("studentId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID студента");
        }

        List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(studentId);

        List<Attendance> allAttendance = new ArrayList<>();
        List<Grade>      allGrades     = new ArrayList<>();

        for (GroupStudent gs : enrollments) {
            allAttendance.addAll(attendanceDAO.findByGroupStudentId(gs.getId()));
            allGrades.addAll(gradeDAO.findByGroupStudentId(gs.getId()));
        }

        List<Payment> payments = paymentDAO.findByStudentId(studentId);

        JsonObject history = new JsonObject();
        history.add("enrollments", gson.toJsonTree(enrollments));
        history.add("attendance",  gson.toJsonTree(allAttendance));
        history.add("grades",      gson.toJsonTree(allGrades));
        history.add("payments",    gson.toJsonTree(payments));

        return Response.ok(history.toString());
    }

    public Response deleteStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID студента не передан");
        }
        int studentId;
        try {
            studentId = JsonParser.parseString(data)
                    .getAsJsonObject().get("studentId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID студента");
        }

        Student student = studentDAO.findById(studentId).orElse(null);
        if (student == null) {
            return Response.error("Студент не найден: " + studentId);
        }

        int personId = student.getPersonId();

        List<GroupStudent> enrollments = groupStudentDAO.findByStudentId(studentId);
        for (GroupStudent gs : enrollments) {
            attendanceDAO.findByGroupStudentId(gs.getId())
                    .forEach(a -> attendanceDAO.delete(a.getId()));
            gradeDAO.findByGroupStudentId(gs.getId())
                    .forEach(g -> gradeDAO.delete(g.getId()));
            groupStudentDAO.delete(gs.getId());
        }
        paymentDAO.findByStudentId(studentId)
                .forEach(p -> paymentDAO.delete(p.getId()));
        studentDAO.delete(studentId);

        personDAO.findById(personId).ifPresent(person -> {
            if (person.getUserId() != null) {
                userDAO.delete(person.getUserId());
            }
            personDAO.delete(personId);
        });

        return Response.ok();
    }

    public Response getStudentsByGroup(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("groupId не передан");
        }
        int groupId;
        try {
            groupId = JsonParser.parseString(data)
                    .getAsJsonObject().get("groupId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный groupId");
        }

        List<GroupStudent> enrollments =
                groupStudentDAO.findByGroupId(groupId).stream()
                        .filter(gs -> gs.getStatus() ==
                                com.bazylev.server.enums.EnrollmentStatus.ACTIVE)
                        .toList();

        com.google.gson.JsonArray result = new com.google.gson.JsonArray();
        for (GroupStudent gs : enrollments) {
            Student student = studentDAO.findById(gs.getStudentId()).orElse(null);
            com.google.gson.JsonObject item = gson.toJsonTree(gs).getAsJsonObject();

            if (student != null) {
                Person person = personDAO.findById(student.getPersonId()).orElse(null);
                if (person != null) {
                    item.addProperty("fullName", person.getFullName());
                } else {
                    item.addProperty("fullName", "Студент #" + gs.getStudentId());
                }
            } else {
                item.addProperty("fullName", "Студент #" + gs.getStudentId());
            }
            result.add(item);
        }
        return Response.ok(result.toString());
    }
}
