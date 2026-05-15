package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.GroupStatus;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.Teacher;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupFormController {

    @FXML private Label    titleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<CourseItem>   courseCombo;
    @FXML private ComboBox<TeacherItem>  teacherCombo;
    @FXML private Spinner<Integer>       maxStudentsSpinner;
    @FXML private DatePicker             startDatePicker;
    @FXML private DatePicker             endDatePicker;
    @FXML private ComboBox<String>       statusCombo;

    private final Gson gson = GsonFactory.getInstance();
    private Group editingGroup;
    private Runnable onSaved;
    private final Map<Integer, String> personNameById = new HashMap<>();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("IN_PROGRESS", "COMPLETED"));
        statusCombo.getSelectionModel().selectFirst();
        loadCourses();
        loadPersons();
        loadTeachers();
    }

    public void setGroup(Group group) {
        this.editingGroup = group;
        titleLabel.setText("Редактировать группу");
        nameField.setText(group.getName());
        maxStudentsSpinner.getValueFactory().setValue(group.getMaxStudents());
        if (group.getStartDate() != null) startDatePicker.setValue(group.getStartDate());
        if (group.getEndDate()   != null) endDatePicker.setValue(group.getEndDate());
        statusCombo.setValue(group.getStatus().name());
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    private void loadCourses() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_COURSES));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Course> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Course>>(){}.getType());
            List<CourseItem> items = list.stream()
                    .map(c -> new CourseItem(c.getId(), c.getName()))
                    .toList();
            courseCombo.setItems(FXCollections.observableArrayList(items));
        }
    }

    private void loadTeachers() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_TEACHERS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Teacher> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Teacher>>(){}.getType());
            List<TeacherItem> items = list.stream()
                    .map(t -> new TeacherItem(t.getId(),
                            personNameById.getOrDefault(
                                    t.getPersonId(),
                                    "Преподаватель #" + t.getId())))
                    .toList();
            teacherCombo.setItems(FXCollections.observableArrayList(items));
        }
    }

    private void loadPersons() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_PERSONS));
        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            List<Person> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Person>>(){}.getType());
            personNameById.clear();
            for (Person p : list) {
                personNameById.put(p.getId(), safeFullName(p));
            }
        }
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   == null ? "" : p.getLastName().strip();
        String fn = p.getFirstName()  == null ? "" : p.getFirstName().strip();
        String mn = p.getMiddleName() == null ? "" : p.getMiddleName().strip();
        String fio = (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
        return fio.isBlank() ? ("Person #" + p.getId()) : fio;
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        JsonObject data = new JsonObject();
        data.addProperty("name",        nameField.getText().strip());
        data.addProperty("courseId",    courseCombo.getValue().id());
        data.addProperty("teacherId",   teacherCombo.getValue().id());
        data.addProperty("maxStudents", maxStudentsSpinner.getValue());
        data.addProperty("status",      statusCombo.getValue());

        if (startDatePicker.getValue() != null)
            data.addProperty("startDate", startDatePicker.getValue().toString());
        if (endDatePicker.getValue() != null)
            data.addProperty("endDate", endDatePicker.getValue().toString());

        RequestType type;
        if (editingGroup != null) {
            data.addProperty("id", editingGroup.getId());
            type = RequestType.UPDATE_GROUP;
        } else {
            type = RequestType.CREATE_GROUP;
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(type, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            if (onSaved != null) onSaved.run();
            close();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (nameField.getText().isBlank()) {
            AlertUtil.showWarning("Валидация", "Введите название группы");
            return false;
        }
        if (courseCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите курс");
            return false;
        }
        if (teacherCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите преподавателя");
            return false;
        }
        return true;
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    record CourseItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    record TeacherItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
