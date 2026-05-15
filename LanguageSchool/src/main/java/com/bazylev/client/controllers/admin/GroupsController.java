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
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.stream.Collectors;

public class GroupsController {

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private Label      countLabel;

    @FXML private TableView<Group>            groupsTable;
    @FXML private TableColumn<Group, String>  colName;
    @FXML private TableColumn<Group, String>  colCourse;
    @FXML private TableColumn<Group, String>  colTeacher;
    @FXML private TableColumn<Group, Integer> colStudents;
    @FXML private TableColumn<Group, Integer> colMaxStudents;
    @FXML private TableColumn<Group, String>  colStatus;
    @FXML private TableColumn<Group, String>  colStartDate;
    @FXML private TableColumn<Group, String>  colEndDate;

    private final Gson gson = GsonFactory.getInstance();
    private ObservableList<Group> allGroups = FXCollections.observableArrayList();
    private FilteredList<Group>   filtered;

    private final Map<Integer, Integer> teacherPersonByTeacherId = new HashMap<>();
    private final Map<Integer, String>  personNameById           = new HashMap<>();
    private final Map<Integer, String>  courseNameById           = new HashMap<>();

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadPersons();
        loadTeachers();
        loadCourses();
        loadGroups();
    }

    private void setupColumns() {
        colName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        colCourse.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        courseNameById.getOrDefault(
                                d.getValue().getCourseId(),
                                "Курс #" + d.getValue().getCourseId())));
        colTeacher.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        resolveTeacherName(d.getValue().getTeacherId())));
        colStudents.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStudentCount()).asObject());
        colMaxStudents.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(
                        d.getValue().getMaxStudents()).asObject());
        colStatus.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getStatus() == GroupStatus.IN_PROGRESS
                                ? "Идёт" : "Завершена"));
        colStartDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getStartDate() != null
                                ? d.getValue().getStartDate().toString() : ""));
        colEndDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getEndDate() != null
                                ? d.getValue().getEndDate().toString() : ""));
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("Все", "Идёт", "Завершена"));
        statusFilter.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(allGroups, g -> true);
        groupsTable.setItems(filtered);
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
        Response response = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (response.getStatus() == ResponseStatus.OK) {
            List<Group> list = gson.fromJson(response.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            allGroups.setAll(list);
            groupsTable.refresh();
            updateCount();
        } else {
            AlertUtil.showError("Ошибка", response.getMessage());
        }
    }

    private void loadTeachers() {
        Response response = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_TEACHERS));
        if (response.getStatus() == ResponseStatus.OK && response.getData() != null) {
            List<Teacher> list = gson.fromJson(response.getData(),
                    new TypeToken<List<Teacher>>(){}.getType());
            teacherPersonByTeacherId.clear();
            for (Teacher t : list) {
                teacherPersonByTeacherId.put(t.getId(), t.getPersonId());
            }
        }
    }

    private void loadPersons() {
        Response response = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_PERSONS));
        if (response.getStatus() == ResponseStatus.OK && response.getData() != null) {
            List<Person> list = gson.fromJson(response.getData(),
                    new TypeToken<List<Person>>(){}.getType());
            personNameById.clear();
            for (Person p : list) {
                personNameById.put(p.getId(), safeFullName(p));
            }
        }
    }

    private String resolveTeacherName(int teacherId) {
        Integer personId = teacherPersonByTeacherId.get(teacherId);
        if (personId == null) return "Преподаватель #" + teacherId;
        return personNameById.getOrDefault(personId, "Преподаватель #" + teacherId);
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   == null ? "" : p.getLastName().strip();
        String fn = p.getFirstName()  == null ? "" : p.getFirstName().strip();
        String mn = p.getMiddleName() == null ? "" : p.getMiddleName().strip();
        String fio = (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
        return fio.isBlank() ? ("Person #" + p.getId()) : fio;
    }

    @FXML private void onSearch()       { applyFilters(); }
    @FXML private void onFilterChange() { applyFilters(); }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        courseFilter.getSelectionModel().clearSelection();
        applyFilters();
    }

    private void applyFilters() {
        String search    = searchField.getText().strip().toLowerCase();
        String statusSel = statusFilter.getValue();

        filtered.setPredicate(group -> {
            boolean matchName = search.isBlank()
                    || group.getName().toLowerCase().contains(search);
            boolean matchStatus = statusSel == null || statusSel.equals("Все")
                    || (statusSel.equals("Идёт")
                            && group.getStatus() == GroupStatus.IN_PROGRESS)
                    || (statusSel.equals("Завершена")
                            && group.getStatus() == GroupStatus.COMPLETED);
            return matchName && matchStatus;
        });
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Всего: " + filtered.size());
    }

    @FXML
    private void onTableClicked(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            onEditGroup();
        }
    }

    @FXML
    private void onCreateGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/bazylev/client/views/admin/group-form.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupFormController ctrl = loader.getController();
            ctrl.setOnSaved(this::loadGroups);

            Stage modal = new Stage();
            modal.setTitle("Создать группу");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEditGroup() {
        Group selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/bazylev/client/views/admin/group-form.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupFormController ctrl = loader.getController();
            ctrl.setGroup(selected);
            ctrl.setOnSaved(this::loadGroups);

            Stage modal = new Stage();
            modal.setTitle("Редактировать группу");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEnrollStudent() {
        Group selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу для зачисления");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/bazylev/client/views/admin/enroll-student.fxml"));
            javafx.scene.Parent root = loader.load();
            EnrollStudentController ctrl = loader.getController();
            ctrl.setGroup(selected);
            ctrl.setOnEnrolled(this::loadGroups);

            Stage modal = new Stage();
            modal.setTitle("Зачисление в группу: " + selected.getName());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onViewSchedule() {
        Group selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу для просмотра расписания");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/group-schedule.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupScheduleController ctrl = loader.getController();
            ctrl.setGroup(selected);

            Stage modal = new Stage();
            modal.setTitle("Расписание — " + selected.getName());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть расписание: " + e.getMessage());
        }
    }

    @FXML
    private void onViewStudents() {
        Group selected = groupsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу для просмотра студентов");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/group-students.fxml"));
            javafx.scene.Parent root = loader.load();
            GroupStudentsController ctrl = loader.getController();
            ctrl.setGroup(selected);

            Stage modal = new Stage();
            modal.setTitle("Студенты — " + selected.getName());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть список студентов: " + e.getMessage());
        }
    }
}
