package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.Student;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.bazylev.client.util.SceneManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentsController {

    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    @FXML private TableView<Student>             table;
    @FXML private TableColumn<Student, Integer>  colId;
    @FXML private TableColumn<Student, String>   colPerson;
    @FXML private TableColumn<Student, String>   colDate;
    @FXML private TableColumn<Student, String>   colStatus;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Student> all      = FXCollections.observableArrayList();
    private       FilteredList<Student>   filtered;
    private final Map<Integer, String>    personNameById = new HashMap<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colPerson.setCellValueFactory(d ->
                new SimpleStringProperty(personNameById.getOrDefault(
                        d.getValue().getPersonId(),
                        "Person #" + d.getValue().getPersonId())));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEnrollmentDate() != null
                        ? d.getValue().getEnrollmentDate().toString() : ""));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isActive()
                        ? "✅ Активен" : "❌ Неактивен"));

        filtered = new FilteredList<>(all, s -> true);
        table.setItems(filtered);
        loadPersons();
        load();
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

    private void load() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_STUDENTS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Student> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Student>>(){}.getType());
            all.setAll(list);
            table.refresh();
            updateCount();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().strip().toLowerCase();
        filtered.setPredicate(s -> {
            if (q.isBlank()) return true;
            String name = personNameById.getOrDefault(
                    s.getPersonId(), "").toLowerCase();
            return name.contains(q)
                    || String.valueOf(s.getId()).contains(q);
        });
        updateCount();
    }

    @FXML
    private void onReset() {
        searchField.clear();
        filtered.setPredicate(s -> true);
        updateCount();
    }

    @FXML
    private void onDoubleClick(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            onEdit();
        }
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.resolveUrl(
                            "/com/bazylev/client/views/admin/student-form.fxml"));
            javafx.scene.Parent root = loader.load();
            StudentFormController ctrl = loader.getController();
            ctrl.setOnSaved(() -> {
                loadPersons();
                load();
            });

            Stage modal = new Stage();
            modal.setTitle("Добавить студента");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEdit() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите студента для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.resolveUrl(
                            "/com/bazylev/client/views/admin/student-edit-form.fxml"));
            javafx.scene.Parent root = loader.load();
            StudentEditFormController ctrl = loader.getController();
            ctrl.setStudent(selected);
            ctrl.setOnSaved(() -> {
                loadPersons();
                load();
            });

            Stage modal = new Stage();
            modal.setTitle("Редактировать студента #" + selected.getId());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите студента для удаления");
            return;
        }
        String name = personNameById.getOrDefault(
                selected.getPersonId(), "Студент #" + selected.getId());
        if (!AlertUtil.showConfirm("Удаление",
                "Удалить студента «" + name + "»?\n"
                        + "Также будет удалена его учётная запись.")) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("studentId", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_STUDENT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            loadPersons();
            load();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onHistory() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите студента");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("studentId", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_STUDENT_HISTORY, data.toString()));

        if (resp.getStatus() != ResponseStatus.OK) {
            AlertUtil.showError("Ошибка", resp.getMessage());
            return;
        }

        com.google.gson.JsonObject history =
                com.google.gson.JsonParser.parseString(resp.getData()).getAsJsonObject();

        int enrollments = history.getAsJsonArray("enrollments").size();
        int attendance  = history.getAsJsonArray("attendance").size();
        int grades      = history.getAsJsonArray("grades").size();
        int payments    = history.getAsJsonArray("payments").size();

        String name = personNameById.getOrDefault(
                selected.getPersonId(), "Студент #" + selected.getId());

        String summary = "Студент: " + name + "\n\n"
                + "Зачислений в группы: " + enrollments + "\n"
                + "Записей посещаемости: " + attendance  + "\n"
                + "Оценок:              " + grades       + "\n"
                + "Платежей:            " + payments;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("История студента");
        alert.setHeaderText(null);
        TextArea area = new TextArea(summary);
        area.setEditable(false);
        area.setPrefSize(360, 200);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void updateCount() {
        countLabel.setText("Всего: " + filtered.size());
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   == null ? "" : p.getLastName().strip();
        String fn = p.getFirstName()  == null ? "" : p.getFirstName().strip();
        String mn = p.getMiddleName() == null ? "" : p.getMiddleName().strip();
        String fio = (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
        return fio.isBlank() ? ("Person #" + p.getId()) : fio;
    }
}