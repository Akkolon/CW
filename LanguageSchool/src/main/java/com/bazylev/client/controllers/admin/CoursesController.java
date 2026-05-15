package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
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
import java.util.List;

public class CoursesController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private CheckBox         activeOnly;
    @FXML private Label            countLabel;

    @FXML private TableView<Course>             table;
    @FXML private TableColumn<Course, Integer>  colId;
    @FXML private TableColumn<Course, String>   colName;
    @FXML private TableColumn<Course, String>   colLevel;
    @FXML private TableColumn<Course, Integer>  colHours;
    @FXML private TableColumn<Course, String>   colPrice;
    @FXML private TableColumn<Course, String>   colActive;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Course> all      = FXCollections.observableArrayList();
    private       FilteredList<Course>   filtered;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));
        colLevel.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLevel() != null
                        ? d.getValue().getLevel() : ""));
        colHours.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getDurationHours()).asObject());
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPricePerMonth() != null
                        ? d.getValue().getPricePerMonth().toPlainString() + " руб." : "—"));
        colActive.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isActive() ? "✅ Да" : "❌ Нет"));

        levelFilter.setItems(FXCollections.observableArrayList(
                "Все", "A1", "A2", "B1", "B2", "C1", "C2",
                "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        levelFilter.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(all, c -> true);
        table.setItems(filtered);
        load();
    }

    private void load() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_COURSES));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Course> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Course>>(){}.getType());
            all.setAll(list);
            applyFilters();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML private void onSearch() { applyFilters(); }
    @FXML private void onFilter() { applyFilters(); }

    @FXML
    private void onReset() {
        searchField.clear();
        levelFilter.getSelectionModel().selectFirst();
        activeOnly.setSelected(true);
        applyFilters();
    }

    private void applyFilters() {
        String q     = searchField.getText().strip().toLowerCase();
        String level = levelFilter.getValue();
        boolean onlyActive = activeOnly.isSelected();

        filtered.setPredicate(c -> {
            boolean matchName   = q.isBlank() || c.getName().toLowerCase().contains(q);
            boolean matchLevel  = level == null || level.equals("Все")
                    || level.equals(c.getLevel());
            boolean matchActive = !onlyActive || c.isActive();
            return matchName && matchLevel && matchActive;
        });
        countLabel.setText("Всего: " + filtered.size());
    }

    @FXML
    private void onDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            onEdit();
        }
    }

    @FXML
    private void onCreate() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите курс для редактирования");
            return;
        }
        openForm(selected);
    }

    @FXML
    private void onDeactivate() {
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите курс");
            return;
        }
        String action = selected.isActive() ? "деактивировать" : "активировать";
        if (!AlertUtil.showConfirm("Подтверждение",
                "Вы хотите " + action + " курс «" + selected.getName() + "»?")) return;

        JsonObject data = new JsonObject();
        data.addProperty("id",     selected.getId());
        data.addProperty("name",   selected.getName());
        data.addProperty("level",  selected.getLevel());
        data.addProperty("durationHours", selected.getDurationHours());
        data.addProperty("active", !selected.isActive());
        if (selected.getPricePerMonth() != null) {
            data.addProperty("pricePerMonth", selected.getPricePerMonth());
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.UPDATE_COURSE, data.toString()));
        if (resp.getStatus() == ResponseStatus.OK) {
            load();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void openForm(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.resolveUrl(
                    "/com/bazylev/client/views/admin/course-form.fxml"));
            javafx.scene.Parent root = loader.load();
            CourseFormController ctrl = loader.getController();
            if (course != null) ctrl.setCourse(course);
            ctrl.setOnSaved(this::load);

            Stage modal = new Stage();
            modal.setTitle(course == null ? "Создать курс" : "Редактировать курс");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }
}