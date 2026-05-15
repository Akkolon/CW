package com.bazylev.client.controllers.teacher;

import com.bazylev.client.controllers.admin.GroupStudentsController;
import com.bazylev.client.enums.GroupStatus;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.bazylev.client.util.SceneManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupsController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label            countLabel;

    @FXML private TableView<Group>             table;
    @FXML private TableColumn<Group, String>   colName;
    @FXML private TableColumn<Group, String>   colCourse;
    @FXML private TableColumn<Group, Integer>  colStudents;
    @FXML private TableColumn<Group, Integer>  colMax;
    @FXML private TableColumn<Group, String>   colStatus;
    @FXML private TableColumn<Group, String>   colStartDate;
    @FXML private TableColumn<Group, String>   colEndDate;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Group> all      = FXCollections.observableArrayList();
    private       FilteredList<Group>   filtered;
    private final Map<Integer, String>  courseNameById = new HashMap<>();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        colCourse.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        courseNameById.getOrDefault(
                                d.getValue().getCourseId(),
                                "Курс #" + d.getValue().getCourseId())));
        colStudents.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStudentCount()).asObject());
        colMax.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getMaxStudents()).asObject());
        colStatus.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getStatus() == GroupStatus.IN_PROGRESS
                                ? "✅ Идёт" : "Завершена"));
        colStartDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getStartDate() != null
                                ? d.getValue().getStartDate().toString() : ""));
        colEndDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getEndDate() != null
                                ? d.getValue().getEndDate().toString() : ""));

        statusFilter.setItems(FXCollections.observableArrayList(
                "Все", "Идёт", "Завершена"));
        statusFilter.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(all, g -> true);
        table.setItems(filtered);

        loadCourses();
        load();
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

    private void load() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Group> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            all.setAll(list);
            applyFilters();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML private void onSearch() { applyFilters(); }
    @FXML private void onFilter() { applyFilters(); }
    @FXML private void onRefresh() { loadCourses(); load(); }

    @FXML
    private void onReset() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        applyFilters();
    }

    private void applyFilters() {
        String q   = searchField.getText().strip().toLowerCase();
        String sel = statusFilter.getValue();
        filtered.setPredicate(g -> {
            boolean matchName   = q.isBlank() || g.getName().toLowerCase().contains(q);
            boolean matchStatus = sel == null || sel.equals("Все")
                    || (sel.equals("Идёт")     && g.getStatus() == GroupStatus.IN_PROGRESS)
                    || (sel.equals("Завершена") && g.getStatus() == GroupStatus.COMPLETED);
            return matchName && matchStatus;
        });
        countLabel.setText("Групп: " + filtered.size());
    }

    @FXML
    private void onDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            onOpenAttendance();
        }
    }

    @FXML private void onOpenAttendance() { navigateToPanel("attendance"); }
    @FXML private void onOpenGrades()     { navigateToPanel("grades"); }
    @FXML private void onOpenSchedule()   { navigateToPanel("schedule"); }

    @FXML
    private void onOpenStudents() {
        Group selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу из списка");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.resolveUrl(
                    "/com/bazylev/client/views/teacher/group-students.fxml"));
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

    private void navigateToPanel(String section) {
        if (table.getSelectionModel().getSelectedItem() == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу из списка");
            return;
        }
        try {
            String path = "/com/bazylev/client/views/teacher/" + section + ".fxml";
            Node content = FXMLLoader.load(SceneManager.resolveUrl(path));

            javafx.scene.Parent parent = table.getScene().getRoot();
            StackPane contentArea = (StackPane) parent.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(content);
                parent.lookupAll(".nav-button-active")
                        .forEach(n -> n.getStyleClass().remove("nav-button-active"));
                javafx.scene.Node navBtn = parent.lookup("#btn" +
                        Character.toUpperCase(section.charAt(0)) + section.substring(1));
                if (navBtn != null) navBtn.getStyleClass().add("nav-button-active");
            }
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть раздел: " + e.getMessage());
        }
    }
}
