package com.bazylev.client.controllers.student;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Schedule;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.session.ClientSession;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentScheduleController {

    @FXML private TableView<Schedule>            table;
    @FXML private TableColumn<Schedule, String>  colGroup;
    @FXML private TableColumn<Schedule, String>  colCourse;
    @FXML private TableColumn<Schedule, String>  colDay;
    @FXML private TableColumn<Schedule, String>  colStart;
    @FXML private TableColumn<Schedule, String>  colEnd;
    @FXML private TableColumn<Schedule, String>  colRoom;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Schedule> items = FXCollections.observableArrayList();

    private final Map<Integer, String> groupNameById  = new HashMap<>();
    private final Map<Integer, String> courseNameById = new HashMap<>();
    private final Map<Integer, Integer> courseByGroup = new HashMap<>();

    @FXML
    public void initialize() {
        colGroup.setCellValueFactory(d ->
                new SimpleStringProperty(
                        groupNameById.getOrDefault(
                                d.getValue().getGroupId(),
                                "Группа #" + d.getValue().getGroupId())));
        colCourse.setCellValueFactory(d -> {
            int groupId  = d.getValue().getGroupId();
            int courseId = courseByGroup.getOrDefault(groupId, -1);
            return new SimpleStringProperty(
                    courseNameById.getOrDefault(courseId, "—"));
        });
        colDay.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDayOfWeek() != null
                                ? formatDay(d.getValue().getDayOfWeek().name()) : ""));
        colStart.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getStartTime() != null
                                ? d.getValue().getStartTime().toString() : ""));
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getEndTime() != null
                                ? d.getValue().getEndTime().toString() : ""));
        colRoom.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getRoom() != null
                                ? d.getValue().getRoom() : ""));

        table.setItems(items);
        loadMeta();
        load();
    }

    @FXML
    private void onRefresh() {
        loadMeta();
        load();
    }

    private void loadMeta() {
        Response groupResp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (groupResp.getStatus() == ResponseStatus.OK && groupResp.getData() != null) {
            List<Group> groups = gson.fromJson(groupResp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            groupNameById.clear();
            courseByGroup.clear();
            for (Group g : groups) {
                groupNameById.put(g.getId(), g.getName());
                courseByGroup.put(g.getId(), g.getCourseId());
            }
        }

        Response courseResp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_COURSES));
        if (courseResp.getStatus() == ResponseStatus.OK && courseResp.getData() != null) {
            List<Course> courses = gson.fromJson(courseResp.getData(),
                    new TypeToken<List<Course>>(){}.getType());
            courseNameById.clear();
            for (Course c : courses) {
                courseNameById.put(c.getId(), c.getName());
            }
        }
    }

    private void load() {
        int studentId = ClientSession.getInstance().getStudentId();
        if (studentId <= 0) {
            AlertUtil.showError("Ошибка", "Не определён ID студента. Попробуйте войти заново.");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("studentId", studentId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_SCHEDULE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Schedule> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Schedule>>(){}.getType());
            items.setAll(list);
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private String formatDay(String day) {
        return switch (day) {
            case "MON" -> "Понедельник";
            case "TUE" -> "Вторник";
            case "WED" -> "Среда";
            case "THU" -> "Четверг";
            case "FRI" -> "Пятница";
            case "SAT" -> "Суббота";
            case "SUN" -> "Воскресенье";
            default    -> day;
        };
    }
}