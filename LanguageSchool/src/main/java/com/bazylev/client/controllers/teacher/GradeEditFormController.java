package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Grade;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class GradeEditFormController {

    @FXML private Label            studentLabel;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        valueField;
    @FXML private DatePicker       datePicker;
    @FXML private Label            errorLabel;

    private Grade    editingGrade;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(
                "HOMEWORK", "TEST", "EXAM", "ACTIVITY"));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    public void setGrade(Grade grade, String studentName) {
        this.editingGrade = grade;
        studentLabel.setText(studentName);
        typeCombo.setValue(grade.getGradeType() != null
                ? grade.getGradeType().name() : "HOMEWORK");
        valueField.setText(String.valueOf(grade.getValue()));
        datePicker.setValue(grade.getGradeDate());
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        double value;
        try {
            value = Double.parseDouble(
                    valueField.getText().strip().replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Некорректный формат оценки");
            return;
        }
        if (value < 0 || value > 10) {
            showError("Оценка должна быть в диапазоне 0–10");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id",        editingGrade.getId());
        data.addProperty("gradeType", typeCombo.getValue());
        data.addProperty("value",     value);
        data.addProperty("gradeDate", datePicker.getValue().toString());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.UPDATE_GRADE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            if (onSaved != null) onSaved.run();
            close();
        } else {
            showError(resp.getMessage() != null
                    ? resp.getMessage() : "Не удалось сохранить");
        }
    }

    @FXML
    private void onCancel() { close(); }

    private boolean validate() {
        if (valueField.getText().isBlank()) {
            showError("Введите значение оценки");
            return false;
        }
        if (datePicker.getValue() == null) {
            showError("Выберите дату");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) typeCombo.getScene().getWindow()).close();
    }
}