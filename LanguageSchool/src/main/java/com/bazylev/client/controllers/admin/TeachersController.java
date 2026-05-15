package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.Teacher;
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

public class TeachersController {

    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    @FXML private TableView<Teacher>             table;
    @FXML private TableColumn<Teacher, Integer>  colId;
    @FXML private TableColumn<Teacher, String>   colPerson;
    @FXML private TableColumn<Teacher, String>   colSpecialization;
    @FXML private TableColumn<Teacher, String>   colHireDate;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Teacher> all      = FXCollections.observableArrayList();
    private       FilteredList<Teacher>   filtered;
    private final Map<Integer, String>    personNameById = new HashMap<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colPerson.setCellValueFactory(d ->
                new SimpleStringProperty(personNameById.getOrDefault(
                        d.getValue().getPersonId(),
                        "Person #" + d.getValue().getPersonId())));
        colSpecialization.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getSpecialization() != null
                        ? d.getValue().getSpecialization() : ""));
        colHireDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getHireDate() != null
                        ? d.getValue().getHireDate().toString() : ""));

        filtered = new FilteredList<>(all, t -> true);
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
                .send(RequestBuilder.of(RequestType.GET_ALL_TEACHERS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Teacher> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Teacher>>(){}.getType());
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
        filtered.setPredicate(t -> {
            if (q.isBlank()) return true;
            String name = personNameById.getOrDefault(
                    t.getPersonId(), "").toLowerCase();
            String spec = t.getSpecialization() != null
                    ? t.getSpecialization().toLowerCase() : "";
            return name.contains(q) || spec.contains(q);
        });
        updateCount();
    }

    @FXML
    private void onReset() {
        searchField.clear();
        filtered.setPredicate(t -> true);
        updateCount();
    }

    @FXML
    private void onDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            onEdit();
        }
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.resolveUrl(
                    "/com/bazylev/client/views/admin/teacher-form.fxml"));
            javafx.scene.Parent root = loader.load();
            TeacherFormController ctrl = loader.getController();
            ctrl.setOnSaved(() -> {
                loadPersons();
                load();
            });

            Stage modal = new Stage();
            modal.setTitle("Добавить преподавателя");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEdit() {
        Teacher selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите преподавателя для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.resolveUrl(
                    "/com/bazylev/client/views/admin/teacher-edit-form.fxml"));
            javafx.scene.Parent root = loader.load();
            TeacherEditFormController ctrl = loader.getController();
            ctrl.setTeacher(selected);
            ctrl.setOnSaved(() -> {
                loadPersons();
                load();
            });

            Stage modal = new Stage();
            modal.setTitle("Редактировать преподавателя #" + selected.getId());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Teacher selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите преподавателя для удаления");
            return;
        }
        String name = personNameById.getOrDefault(
                selected.getPersonId(), "Преподаватель #" + selected.getId());
        if (!AlertUtil.showConfirm("Удаление",
                "Удалить преподавателя «" + name + "»?\n"
                        + "Также будет удалена его учётная запись.\n"
                        + "Группы преподавателя останутся, но останутся без преподавателя.")) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("teacherId", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_TEACHER, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            loadPersons();
            load();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onSchedule() {
        Teacher selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите преподавателя");
            return;
        }
        AlertUtil.showInfo("Расписание",
                "Расписание преподавателя #" + selected.getId()
                        + "\nОткройте раздел «Расписание» для подробного просмотра.");
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