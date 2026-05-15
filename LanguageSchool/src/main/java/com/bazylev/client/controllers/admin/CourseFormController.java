package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Course;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class CourseFormController {

    @FXML private Label         titleLabel;
    @FXML private TextField     nameField;
    @FXML private TextField     descField;
    @FXML private ComboBox<String> levelCombo;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private TextField     priceField;
    @FXML private CheckBox      activeCheck;

    private Course   editingCourse;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        levelCombo.setItems(FXCollections.observableArrayList(
                "A1", "A2", "B1", "B2", "C1", "C2",
                "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        levelCombo.getSelectionModel().selectFirst();
    }

    public void setCourse(Course course) {
        this.editingCourse = course;
        titleLabel.setText("Редактировать курс");
        nameField.setText(course.getName());
        descField.setText(course.getDescription() != null ? course.getDescription() : "");
        if (course.getLevel() != null) levelCombo.setValue(course.getLevel());
        hoursSpinner.getValueFactory().setValue(course.getDurationHours());
        if (course.getPricePerMonth() != null) {
            priceField.setText(course.getPricePerMonth().toPlainString());
        }
        activeCheck.setSelected(course.isActive());
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        JsonObject data = new JsonObject();
        data.addProperty("name",          nameField.getText().strip());
        data.addProperty("description",   descField.getText().strip());
        data.addProperty("level",         levelCombo.getValue());
        data.addProperty("durationHours", hoursSpinner.getValue());
        data.addProperty("active",        activeCheck.isSelected());

        String priceStr = priceField.getText().strip();
        if (!priceStr.isBlank()) {
            try {
                data.addProperty("pricePerMonth",
                        new BigDecimal(priceStr.replace(",", ".")));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Валидация", "Некорректный формат цены");
                return;
            }
        }

        RequestType type;
        if (editingCourse != null) {
            data.addProperty("id", editingCourse.getId());
            type = RequestType.UPDATE_COURSE;
        } else {
            type = RequestType.CREATE_COURSE;
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
    private void onCancel() { close(); }

    private boolean validate() {
        if (nameField.getText().isBlank()) {
            AlertUtil.showWarning("Валидация", "Введите название курса");
            return false;
        }
        if (levelCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите уровень");
            return false;
        }
        return true;
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}