package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Attendance;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AttendanceEditFormController {

    @FXML private Label            studentLabel;
    @FXML private DatePicker       datePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField        commentField;
    @FXML private Label            errorLabel;

    private Attendance editingAttendance;
    private Runnable   onSaved;

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(
                "PRESENT", "LATE", "ABSENT", "EXCUSED"));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    public void setAttendance(Attendance attendance, String studentName) {
        this.editingAttendance = attendance;
        studentLabel.setText(studentName);
        datePicker.setValue(attendance.getLessonDate());
        statusCombo.setValue(attendance.getStatus() != null
                ? attendance.getStatus().name() : "PRESENT");
        commentField.setText(attendance.getComment() != null
                ? attendance.getComment() : "");
    }

    @FXML
    private void onSave() {
        if (datePicker.getValue() == null) {
            showError("Выберите дату");
            return;
        }
        if (statusCombo.getValue() == null) {
            showError("Выберите статус");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id",         editingAttendance.getId());
        data.addProperty("status",     statusCombo.getValue());
        data.addProperty("lessonDate", datePicker.getValue().toString());
        data.addProperty("comment",    commentField.getText().strip());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.UPDATE_ATTENDANCE, data.toString()));

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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) statusCombo.getScene().getWindow()).close();
    }
}