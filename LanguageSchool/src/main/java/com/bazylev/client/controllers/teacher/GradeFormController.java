package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class GradeFormController {

    @FXML private ComboBox<StudentItem> studentCombo;
    @FXML private ComboBox<String>      typeCombo;
    @FXML private TextField             valueField;
    @FXML private DatePicker            datePicker;
    @FXML private TextField             commentField;
    @FXML private Label                 errorLabel;

    private int      groupId;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(
                "HOMEWORK", "TEST", "EXAM", "ACTIVITY"));
        typeCombo.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setStudents(List<StudentItem> students) {
        studentCombo.setItems(FXCollections.observableArrayList(students));
        if (!studentCombo.getItems().isEmpty()) {
            studentCombo.getSelectionModel().selectFirst();
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
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
        data.addProperty("groupStudentId", studentCombo.getValue().gsId());
        data.addProperty("gradeType",      typeCombo.getValue());
        data.addProperty("value",          value);
        data.addProperty("gradeDate",      datePicker.getValue().toString());
        if (!commentField.getText().isBlank()) {
            data.addProperty("comment", commentField.getText().strip());
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.SET_GRADE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            AlertUtil.showInfo("Успех", "Оценка выставлена");
            if (onSaved != null) onSaved.run();
            close();
        } else {
            showError(resp.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (studentCombo.getValue() == null) {
            showError("Выберите студента");
            return false;
        }
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
        errorLabel.setText(message != null ? message : "Неизвестная ошибка");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) typeCombo.getScene().getWindow()).close();
    }
}