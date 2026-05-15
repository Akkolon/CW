package com.bazylev.server.services;

import com.bazylev.server.dao.CourseDAO;
import com.bazylev.server.models.entities.Course;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.List;

public class CourseService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final Gson      gson      = GsonFactory.getInstance();

    public Response getAllCourses() {
        List<Course> courses = courseDAO.findAll();
        return Response.ok(gson.toJson(courses));
    }

    public Response createCourse(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные курса не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        String name  = json.has("name")  ? json.get("name").getAsString()  : "";
        String level = json.has("level") ? json.get("level").getAsString() : "";
        if (name.isBlank() || level.isBlank()) {
            return Response.error("Название и уровень курса обязательны");
        }

        Course course = new Course();
        course.setName(name);
        course.setDescription(json.has("description") ? json.get("description").getAsString() : "");
        course.setLevel(level);
        course.setDurationHours(json.has("durationHours") ? json.get("durationHours").getAsInt() : 0);
        course.setPricePerMonth(json.has("pricePerMonth")
                ? json.get("pricePerMonth").getAsBigDecimal() : BigDecimal.ZERO);
        course.setActive(true);

        int id = courseDAO.save(course);
        course.setId(id);
        return Response.ok(gson.toJson(course));
    }

    public Response updateCourse(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные курса не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int id = json.has("id") ? json.get("id").getAsInt() : -1;
        if (id < 0) return Response.error("Не указан ID курса");

        Course course = courseDAO.findById(id).orElse(null);
        if (course == null) return Response.error("Курс не найден: " + id);

        if (json.has("name"))          course.setName(json.get("name").getAsString());
        if (json.has("description"))   course.setDescription(json.get("description").getAsString());
        if (json.has("level"))         course.setLevel(json.get("level").getAsString());
        if (json.has("durationHours")) course.setDurationHours(json.get("durationHours").getAsInt());
        if (json.has("pricePerMonth")) course.setPricePerMonth(json.get("pricePerMonth").getAsBigDecimal());
        if (json.has("active"))        course.setActive(json.get("active").getAsBoolean());

        courseDAO.update(course);
        return Response.ok(gson.toJson(course));
    }

    public Response deleteCourse(String data) {
        try {
            int id = JsonParser.parseString(data).getAsJsonObject().get("id").getAsInt();
            courseDAO.delete(id);
            return Response.ok();
        } catch (Exception e) {
            return Response.error("Некорректный ID курса");
        }
    }
}