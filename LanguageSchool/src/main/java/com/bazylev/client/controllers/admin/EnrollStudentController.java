package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.Student;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrollStudentController {

    @FXML private Label     titleLabel;
    @FXML private TextField searchField;
    @FXML private Label     groupInfoLabel;
    @FXML private Label     slotsLabel;

    @FXML private TableView<Student>             studentsTable;
    @FXML private TableColumn<Student, Integer>  colId;
    @FXML private TableColumn<Student, String>   colName;
    @FXML private TableColumn<Student, String>   colStatus;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private FilteredList<Student>         filtered;
    private final Map<Integer, String>    personNameById = new HashMap<>();

    private Group    group;
    private Runnable onEnrolled;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(personNameById.getOrDefault(
                        d.getValue().getPersonId(),
                        "Студент #" + d.getValue().getId())));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().isActive() ? "✅ Активен" : "❌ Неактивен"));

        filtered = new FilteredList<>(allStudents, s -> true);
        studentsTable.setItems(filtered);

        // поиск срабатывает на каждое изменение текста
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        loadPersons();
        loadStudents();
    }

    public void setGroup(Group group) {
        this.group = group;
        titleLabel.setText("Зачислить в: " + group.getName());
        groupInfoLabel.setText(group.getName());
        slotsLabel.setText("Максимум студентов: " + group.getMaxStudents());
    }

    public void setOnEnrolled(Runnable callback) {
        this.onEnrolled = callback;
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

    private void loadStudents() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_STUDENTS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Student> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Student>>(){}.getType());
            // показываем только активных студентов
            allStudents.setAll(list.stream()
                    .filter(Student::isActive)
                    .toList());
            studentsTable.refresh();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            filtered.setPredicate(s -> true);
            return;
        }
        String q = query.strip().toLowerCase();
        filtered.setPredicate(s -> {
            String name = personNameById.getOrDefault(
                    s.getPersonId(), "").toLowerCase();
            return name.contains(q)
                    || String.valueOf(s.getId()).contains(q);
        });
    }

    // кнопка "Найти" теперь просто применяет фильтр явно
    @FXML
    private void onSearch() {
        applyFilter(searchField.getText());
    }

    @FXML
    private void onEnroll() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите студента для зачисления");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("groupId",   group.getId());
        data.addProperty("studentId", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.ENROLL_STUDENT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            String name = personNameById.getOrDefault(
                    selected.getPersonId(), "Студент #" + selected.getId());
            AlertUtil.showInfo("Успех",
                    "Студент «" + name + "» успешно зачислен в группу");
            if (onEnrolled != null) onEnrolled.run();
            close();
        } else {
            AlertUtil.showError("Ошибка зачисления", resp.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) searchField.getScene().getWindow()).close();
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   == null ? "" : p.getLastName().strip();
        String fn = p.getFirstName()  == null ? "" : p.getFirstName().strip();
        String mn = p.getMiddleName() == null ? "" : p.getMiddleName().strip();
        String fio = (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
        return fio.isBlank() ? ("Person #" + p.getId()) : fio;
    }
}