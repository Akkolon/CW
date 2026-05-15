package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Teacher;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TeacherFormController {

    @FXML private Label         titleLabel;
    @FXML private TextField     lastNameField;
    @FXML private TextField     firstNameField;
    @FXML private TextField     middleNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     specializationField;
    @FXML private TextField     loginField;
    @FXML private PasswordField passwordField;

    private Teacher  editingTeacher;
    private Runnable onSaved;

    public void setTeacher(Teacher teacher) {
        this.editingTeacher = teacher;
        titleLabel.setText("Редактировать преподавателя");
        specializationField.setText(
                teacher.getSpecialization() != null ? teacher.getSpecialization() : "");
        // Личные данные не подгружаем — только специализацию (упрощение)
        loginField.setDisable(true);
        passwordField.setDisable(true);
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        JsonObject data = new JsonObject();

        if (editingTeacher != null) {
            data.addProperty("id",             editingTeacher.getId());
            data.addProperty("specialization", specializationField.getText().strip());
            if (!firstNameField.getText().isBlank())
                data.addProperty("firstName",  firstNameField.getText().strip());
            if (!lastNameField.getText().isBlank())
                data.addProperty("lastName",   lastNameField.getText().strip());
            if (!middleNameField.getText().isBlank())
                data.addProperty("middleName", middleNameField.getText().strip());
            if (!emailField.getText().isBlank())
                data.addProperty("email",      emailField.getText().strip());

            send(RequestType.UPDATE_TEACHER, data);
        } else {
            data.addProperty("firstName",      firstNameField.getText().strip());
            data.addProperty("lastName",       lastNameField.getText().strip());
            data.addProperty("middleName",     middleNameField.getText().strip());
            data.addProperty("email",          emailField.getText().strip());
            data.addProperty("specialization", specializationField.getText().strip());
            data.addProperty("login",          loginField.getText().strip());
            data.addProperty("password",       passwordField.getText());

            send(RequestType.CREATE_TEACHER, data);
        }
    }

    private void send(RequestType type, JsonObject data) {
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
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (editingTeacher == null) {
            if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
                AlertUtil.showWarning("Валидация", "Введите имя и фамилию");
                return false;
            }
            if (loginField.getText().isBlank() || passwordField.getText().isBlank()) {
                AlertUtil.showWarning("Валидация", "Введите логин и пароль");
                return false;
            }
        }
        return true;
    }

    private void close() {
        ((Stage) specializationField.getScene().getWindow()).close();
    }
}