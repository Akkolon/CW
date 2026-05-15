package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.GroupStatus;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Student;
import com.bazylev.client.models.entities.Teacher;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label groupsCountLabel;
    @FXML private Label studentsCountLabel;
    @FXML private Label teachersCountLabel;
    @FXML private Label debtorsCountLabel;
    @FXML private Label dateLabel;

    @FXML private TableView<Group>             groupsTable;
    @FXML private TableColumn<Group, String>   colGroupName;
    @FXML private TableColumn<Group, String>   colGroupCourse;
    @FXML private TableColumn<Group, Integer>  colGroupStudents;
    @FXML private TableColumn<Group, String>   colGroupStatus;

    @FXML private ListView<String> debtorsList;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Group>  groups  = FXCollections.observableArrayList();
    private final ObservableList<String> debtors = FXCollections.observableArrayList();

    private final Map<Integer, String> courseNameById = new HashMap<>();

    @FXML
    public void initialize() {
        dateLabel.setText("Сегодня: " + LocalDate.now());

        colGroupName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));
        colGroupCourse.setCellValueFactory(d ->
                new SimpleStringProperty(
                        courseNameById.getOrDefault(
                                d.getValue().getCourseId(),
                                "Курс #" + d.getValue().getCourseId())));
        colGroupStudents.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStudentCount()).asObject());
        colGroupStatus.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getStatus() == GroupStatus.IN_PROGRESS
                                ? "Идёт" : "Завершена"));

        groupsTable.setItems(groups);
        debtorsList.setItems(debtors);
        loadAll();
    }

    @FXML
    private void onRefresh() {
        loadAll();
    }

    private void loadAll() {
        loadCourses();
        loadGroups();
        loadStudents();
        loadTeachers();
        loadDebtors();
    }

    private void loadCourses() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_COURSES));
        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            List<Course> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Course>>(){}.getType());
            courseNameById.clear();
            for (Course c : list) {
                courseNameById.put(c.getId(), c.getName());
            }
        }
    }

    private void loadGroups() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Group> all = gson.fromJson(resp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            List<Group> active = all.stream()
                    .filter(g -> g.getStatus() == GroupStatus.IN_PROGRESS)
                    .toList();
            groups.setAll(active);
            groupsTable.refresh();
            groupsCountLabel.setText(String.valueOf(active.size()));
        }
    }

    private void loadStudents() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_STUDENTS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Student> all = gson.fromJson(resp.getData(),
                    new TypeToken<List<Student>>(){}.getType());
            long active = all.stream().filter(Student::isActive).count();
            studentsCountLabel.setText(String.valueOf(active));
        }
    }

    private void loadTeachers() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_TEACHERS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Teacher> all = gson.fromJson(resp.getData(),
                    new TypeToken<List<Teacher>>(){}.getType());
            teachersCountLabel.setText(String.valueOf(all.size()));
        }
    }

    private void loadDebtors() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_DEBTORS));
        if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) {
            debtorsCountLabel.setText("0");
            return;
        }
        try {
            JsonArray arr = JsonParser.parseString(resp.getData()).getAsJsonArray();
            debtors.clear();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String fullName  = obj.has("fullName")
                        ? obj.get("fullName").getAsString()
                        : "Студент #" + obj.get("studentId").getAsInt();
                String courseName = obj.has("courseName")
                        ? obj.get("courseName").getAsString() : "—";
                String debt = obj.has("debt")
                        ? obj.get("debt").getAsBigDecimal().toPlainString() : "0";
                debtors.add(fullName + " (" + courseName + ") — " + debt + " руб.");
            }
            debtorsCountLabel.setText(String.valueOf(arr.size()));
        } catch (Exception e) {
            debtorsCountLabel.setText("—");
        }
    }
}
