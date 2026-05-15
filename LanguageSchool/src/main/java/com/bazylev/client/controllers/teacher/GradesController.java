package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.GradeType;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Grade;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.GroupStudentInfo;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public class GradesController {

    @FXML private ComboBox<GroupItem>         groupCombo;
    @FXML private ComboBox<StudentItem>       studentCombo;
    @FXML private ComboBox<String>            typeCombo;
    @FXML private DatePicker                  fromDate;
    @FXML private DatePicker                  toDate;
    @FXML private Label                       countLabel;
    @FXML private Label                       averageLabel;

    @FXML private TableView<Grade>             table;
    @FXML private TableColumn<Grade, String>   colStudent;
    @FXML private TableColumn<Grade, String>   colType;
    @FXML private TableColumn<Grade, String>   colValue;
    @FXML private TableColumn<Grade, String>   colDate;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Grade>   allGrades = FXCollections.observableArrayList();
    private       FilteredList<Grade>     filtered;
    private final Map<Integer, String>    studentNameByGsId = new HashMap<>();

    @FXML
    public void initialize() {
        setupColumns();

        filtered = new FilteredList<>(allGrades, g -> true);
        table.setItems(filtered);

        typeCombo.setItems(FXCollections.observableArrayList(
                "Все", "HOMEWORK", "TEST", "EXAM", "ACTIVITY"));
        typeCombo.getSelectionModel().selectFirst();

        studentCombo.setDisable(true);

        loadGroups();
    }

    private void setupColumns() {
        colStudent.setCellValueFactory(d ->
                new SimpleStringProperty(
                        studentNameByGsId.getOrDefault(
                                d.getValue().getGroupStudentId(),
                                "Студент #" + d.getValue().getGroupStudentId())));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(formatType(d.getValue().getGradeType())));
        colValue.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("%.1f", d.getValue().getValue())));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getGradeDate() != null
                        ? d.getValue().getGradeDate().toString() : ""));
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
    private void onGroupSelected() {
        if (groupCombo.getValue() == null) return;
        loadStudentsForGroup(groupCombo.getValue().id());
        loadGrades();
    }

    private void loadStudentsForGroup(int groupId) {
        studentNameByGsId.clear();

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.GET_STUDENTS_BY_GROUP, data.toString()));

        List<StudentItem> items = new java.util.ArrayList<>();
        items.add(new StudentItem(-1, -1, "Все студенты"));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<GroupStudentInfo> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<GroupStudentInfo>>(){}.getType());
            for (GroupStudentInfo gs : list) {
                studentNameByGsId.put(gs.getId(), gs.getFullName());
                items.add(new StudentItem(gs.getId(), gs.getStudentId(),
                        gs.getFullName()));
            }
        }

        studentCombo.setItems(FXCollections.observableArrayList(items));
        studentCombo.getSelectionModel().selectFirst();
        studentCombo.setDisable(false);
    }

    @FXML
    private void onStudentSelected() { applyFilters(); }

    @FXML
    private void onTypeSelected()    { applyFilters(); }

    @FXML
    private void onSearch()          { loadGrades(); }

    @FXML
    private void onReset() {
        typeCombo.getSelectionModel().selectFirst();
        if (studentCombo.getItems() != null && !studentCombo.getItems().isEmpty()) {
            studentCombo.getSelectionModel().selectFirst();
        }
        fromDate.setValue(null);
        toDate.setValue(null);
        allGrades.clear();
        updateStats();
    }

    private void loadGrades() {
        if (groupCombo.getValue() == null) return;

        LocalDate from = fromDate.getValue() != null
                ? fromDate.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate to   = toDate.getValue()   != null
                ? toDate.getValue()   : LocalDate.now();

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupCombo.getValue().id());
        data.addProperty("from",    from.toString());
        data.addProperty("to",      to.toString());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_GRADES, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Grade> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Grade>>(){}.getType());
            allGrades.setAll(list);
            applyFilters();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void applyFilters() {
        StudentItem selectedStudent = studentCombo.getValue();
        String      selectedType   = typeCombo.getValue();

        filtered.setPredicate(grade -> {
            // фильтр по студенту
            boolean matchStudent = selectedStudent == null
                    || selectedStudent.gsId() == -1
                    || grade.getGroupStudentId() == selectedStudent.gsId();

            // фильтр по типу
            boolean matchType = selectedType == null
                    || selectedType.equals("Все")
                    || (grade.getGradeType() != null
                    && grade.getGradeType().name().equals(selectedType));

            return matchStudent && matchType;
        });

        updateStats();
    }

    private void updateStats() {
        List<Grade> visible = filtered.stream().toList();
        countLabel.setText("Записей: " + visible.size());

        OptionalDouble avg = visible.stream()
                .mapToDouble(Grade::getValue)
                .average();
        averageLabel.setText(avg.isPresent()
                ? String.format("Средний балл: %.2f", avg.getAsDouble())
                : "Средний балл: —");
    }

    @FXML
    private void onAddGrade() {
        if (groupCombo.getValue() == null) {
            AlertUtil.showWarning("Выбор", "Сначала выберите группу");
            return;
        }
        if (studentCombo.getItems() == null || studentCombo.getItems().size() <= 1) {
            AlertUtil.showWarning("Выбор", "В группе нет студентов");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/teacher/grade-form.fxml"));
            javafx.scene.Parent root = loader.load();
            GradeFormController ctrl = loader.getController();
            ctrl.setGroupId(groupCombo.getValue().id());
            ctrl.setStudents(studentCombo.getItems().stream()
                    .filter(s -> s.gsId() != -1)
                    .toList());
            ctrl.setOnSaved(this::loadGrades);

            Stage modal = new Stage();
            modal.setTitle("Выставить оценку");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    private String formatType(GradeType type) {
        if (type == null) return "";
        return switch (type) {
            case HOMEWORK -> "Домашнее задание";
            case TEST     -> "Тест";
            case EXAM     -> "Экзамен";
            case ACTIVITY -> "Активность";
        };
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    @FXML
    private void onEditGrade() {
        Grade selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите оценку для редактирования");
            return;
        }
        String studentName = studentNameByGsId.getOrDefault(
                selected.getGroupStudentId(),
                "Студент #" + selected.getGroupStudentId());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/teacher/grade-edit-form.fxml"));
            javafx.scene.Parent root = loader.load();
            GradeEditFormController ctrl = loader.getController();
            ctrl.setGrade(selected, studentName);
            ctrl.setOnSaved(this::loadGrades);

            Stage modal = new Stage();
            modal.setTitle("Редактировать оценку");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteGrade() {
        Grade selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите оценку для удаления");
            return;
        }
        String name = studentNameByGsId.getOrDefault(
                selected.getGroupStudentId(),
                "Студент #" + selected.getGroupStudentId());

        if (!AlertUtil.showConfirm("Удаление",
                "Удалить оценку студента «" + name + "»?\n"
                        + "Тип: " + selected.getGradeType()
                        + ", значение: " + selected.getValue())) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_GRADE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            loadGrades();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }
}