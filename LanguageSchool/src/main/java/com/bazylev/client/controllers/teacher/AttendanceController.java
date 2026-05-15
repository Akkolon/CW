package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.GroupStudentInfo;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceController {

    @FXML private ComboBox<GroupItem> groupCombo;
    @FXML private DatePicker          datePicker;
    @FXML private Label               statusLabel;

    @FXML private TableView<AttendanceRow>             table;
    @FXML private TableColumn<AttendanceRow, Integer>  colId;
    @FXML private TableColumn<AttendanceRow, String>   colName;
    @FXML private TableColumn<AttendanceRow, String>   colStatus;
    @FXML private TableColumn<AttendanceRow, String>   colComment;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
    private final Map<Integer, Integer> existingAttendanceIds = new HashMap<>();

    @FXML
    public void initialize() {
        setupColumns();
        table.setItems(rows);
        datePicker.setValue(LocalDate.now());
        loadGroups();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().groupStudentId()).asObject());
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().fullName()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(
                    FXCollections.observableArrayList(
                            "PRESENT", "LATE", "ABSENT", "EXCUSED"));
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AttendanceRow row = getTableView().getItems().get(getIndex());
                combo.setValue(row.status());
                combo.setOnAction(e -> {
                    AttendanceRow updated = new AttendanceRow(
                            row.groupStudentId(), row.fullName(),
                            combo.getValue(), row.comment());
                    getTableView().getItems().set(getIndex(), updated);
                });
                setGraphic(combo);
            }
        });

        colComment.setCellFactory(col -> new TableCell<>() {
            private final TextField field = new TextField();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AttendanceRow row = getTableView().getItems().get(getIndex());
                field.setText(row.comment());
                field.focusedProperty().addListener((obs, wasFocused, focused) -> {
                    if (!focused) {
                        AttendanceRow updated = new AttendanceRow(
                                row.groupStudentId(), row.fullName(),
                                row.status(), field.getText());
                        getTableView().getItems().set(getIndex(), updated);
                    }
                });
                setGraphic(field);
            }
        });
    }

    private void loadGroups() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Group> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            List<GroupItem> items = list.stream()
                    .map(g -> new GroupItem(g.getId(), g.getName()))
                    .toList();
            groupCombo.setItems(FXCollections.observableArrayList(items));
        }
    }

    @FXML
    private void onGroupSelected() { loadStudents(); }

    @FXML
    private void onDateSelected()  { loadStudents(); }

    private void loadStudents() {
        if (groupCombo.getValue() == null) return;
        int groupId = groupCombo.getValue().id();

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.GET_STUDENTS_BY_GROUP, data.toString()));

        rows.clear();
        existingAttendanceIds.clear();

        if (resp.getStatus() == ResponseStatus.OK) {
            List<GroupStudentInfo> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<GroupStudentInfo>>(){}.getType());
            for (GroupStudentInfo gs : list) {
                rows.add(new AttendanceRow(
                        gs.getId(), gs.getFullName(), "PRESENT", ""));
            }
            statusLabel.setText("Студентов: " + rows.size());
            loadExistingAttendance(groupId);
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void loadExistingAttendance(int groupId) {
        if (datePicker.getValue() == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupId);
        data.addProperty("from",    datePicker.getValue().toString());
        data.addProperty("to",      datePicker.getValue().toString());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.GET_ATTENDANCE, data.toString()));

        if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) return;

        List<com.bazylev.client.models.entities.Attendance> existing =
                gson.fromJson(resp.getData(),
                        new TypeToken<List<com.bazylev.client.models.entities.Attendance>>(){}.getType());

        for (com.bazylev.client.models.entities.Attendance att : existing) {
            existingAttendanceIds.put(att.getGroupStudentId(), att.getId());
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).groupStudentId() == att.getGroupStudentId()) {
                    rows.set(i, new AttendanceRow(
                            att.getGroupStudentId(),
                            rows.get(i).fullName(),
                            att.getStatus() != null ? att.getStatus().name() : "PRESENT",
                            att.getComment() != null ? att.getComment() : ""));
                    break;
                }
            }
        }
    }

    @FXML
    private void onMarkAll() {
        List<AttendanceRow> updated = rows.stream()
                .map(r -> new AttendanceRow(
                        r.groupStudentId(), r.fullName(), "PRESENT", r.comment()))
                .toList();
        rows.setAll(updated);
    }

    @FXML
    private void onSave() {
        if (rows.isEmpty()) {
            AlertUtil.showWarning("Нет данных", "Загрузите студентов группы");
            return;
        }
        if (datePicker.getValue() == null) {
            AlertUtil.showWarning("Дата", "Выберите дату занятия");
            return;
        }

        JsonArray records = new JsonArray();
        for (AttendanceRow row : rows) {
            JsonObject rec = new JsonObject();
            rec.addProperty("groupStudentId", row.groupStudentId());
            rec.addProperty("status",         row.status());
            rec.addProperty("lessonDate",      datePicker.getValue().toString());
            if (row.comment() != null && !row.comment().isBlank()) {
                rec.addProperty("comment", row.comment());
            }
            records.add(rec);
        }

        JsonObject payload = new JsonObject();
        payload.add("records", records);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.MARK_ATTENDANCE, payload.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            AlertUtil.showInfo("Сохранено", "Посещаемость отмечена успешно");
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    record AttendanceRow(int groupStudentId, String fullName,
                         String status, String comment) {}

    @FXML
    private void onEditSelected() {
        AttendanceRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите строку для редактирования");
            return;
        }

        Integer attId = existingAttendanceIds.get(selected.groupStudentId());
        if (attId == null) {
            AlertUtil.showWarning("Нет записи",
                    "Для этого студента ещё нет сохранённой записи на выбранную дату.\n"
                            + "Сначала сохраните журнал кнопкой «Сохранить».");
            return;
        }

        com.bazylev.client.models.entities.Attendance att =
                new com.bazylev.client.models.entities.Attendance();
        att.setId(attId);
        att.setGroupStudentId(selected.groupStudentId());
        att.setLessonDate(datePicker.getValue());
        try {
            att.setStatus(com.bazylev.client.enums.AttendanceStatus.valueOf(
                    selected.status()));
        } catch (Exception ignored) {}
        att.setComment(selected.comment());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/teacher/attendance-edit-form.fxml"));
            javafx.scene.Parent root = loader.load();
            AttendanceEditFormController ctrl = loader.getController();
            ctrl.setAttendance(att, selected.fullName());
            ctrl.setOnSaved(() -> loadExistingAttendance(groupCombo.getValue().id()));

            Stage modal = new Stage();
            modal.setTitle("Редактировать запись");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteSelected() {
        AttendanceRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите строку для удаления");
            return;
        }

        Integer attId = existingAttendanceIds.get(selected.groupStudentId());
        if (attId == null) {
            AlertUtil.showWarning("Нет записи",
                    "Для этого студента нет сохранённой записи на выбранную дату.");
            return;
        }

        if (!AlertUtil.showConfirm("Удаление",
                "Удалить запись посещаемости для «" + selected.fullName() + "»?")) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", attId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_ATTENDANCE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            existingAttendanceIds.remove(selected.groupStudentId());
            // сбрасываем строку на дефолтный статус
            int idx = rows.indexOf(selected);
            if (idx >= 0) {
                rows.set(idx, new AttendanceRow(
                        selected.groupStudentId(), selected.fullName(),
                        "PRESENT", ""));
            }
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }
}