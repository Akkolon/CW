package com.bazylev.client.controllers.admin;

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

public class UserFormController {

    @FXML private ComboBox<String>  roleCombo;
    @FXML private TextField         lastNameField;
    @FXML private TextField         firstNameField;
    @FXML private TextField         middleNameField;
    @FXML private TextField         emailField;
    @FXML private TextField         loginField;
    @FXML private PasswordField     passwordField;
    @FXML private Label             specializationLabel;
    @FXML private TextField         specializationField;
    @FXML private Label             errorLabel;

    private Runnable onSaved;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "TEACHER"));
        roleCombo.getSelectionModel().selectFirst();
        updateSpecializationVisibility();
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onRoleChanged() {
        updateSpecializationVisibility();
        hideError();
    }

    private void updateSpecializationVisibility() {
        boolean isTeacher = "TEACHER".equals(roleCombo.getValue());
        specializationLabel.setVisible(isTeacher);
        specializationLabel.setManaged(isTeacher);
        specializationField.setVisible(isTeacher);
        specializationField.setManaged(isTeacher);
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        String role = roleCombo.getValue();

        JsonObject userData = new JsonObject();
        userData.addProperty("login",      loginField.getText().strip());
        userData.addProperty("password",   passwordField.getText());
        userData.addProperty("role",       role);
        userData.addProperty("firstName",  firstNameField.getText().strip());
        userData.addProperty("lastName",   lastNameField.getText().strip());

        String middle = middleNameField.getText().strip();
        if (!middle.isBlank()) {
            userData.addProperty("middleName", middle);
        }

        String email = emailField.getText().strip();
        if (!email.isBlank()) {
            userData.addProperty("email", email);
        }

        Response userResp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.CREATE_USER, userData.toString()));

        if (userResp.getStatus() != ResponseStatus.OK) {
            showError(userResp.getMessage());
            return;
        }

        if ("TEACHER".equals(role)) {
            int personId = extractPersonId(userResp.getData());
            if (personId < 0) {
                showError("Пользователь создан, но не удалось получить personId для создания преподавателя.");
                return;
            }

            JsonObject teacherData = new JsonObject();
            teacherData.addProperty("personId",       personId);
            teacherData.addProperty("specialization", specializationField.getText().strip());

            Response teacherResp = ServerConnection.getInstance()
                    .send(RequestBuilder.ofRaw(RequestType.CREATE_TEACHER, teacherData.toString()));

            if (teacherResp.getStatus() != ResponseStatus.OK) {
                showError("Пользователь создан, но не удалось создать преподавателя: "
                        + teacherResp.getMessage());
                return;
            }
        }

        AlertUtil.showInfo("Успех", "Пользователь успешно создан");
        if (onSaved != null) onSaved.run();
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (roleCombo.getValue() == null) {
            showError("Выберите роль пользователя");
            return false;
        }
        if (lastNameField.getText().isBlank() || firstNameField.getText().isBlank()) {
            showError("Введите имя и фамилию");
            return false;
        }
        if (loginField.getText().isBlank()) {
            showError("Введите логин");
            return false;
        }
        if (passwordField.getText().isBlank()) {
            showError("Введите пароль");
            return false;
        }
        if (passwordField.getText().length() < 4) {
            showError("Пароль должен содержать не менее 4 символов");
            return false;
        }
        return true;
    }

    private int extractPersonId(String data) {
        if (data == null || data.isBlank()) return -1;
        try {
            var json = com.google.gson.JsonParser.parseString(data).getAsJsonObject();
            if (json.has("personId") && !json.get("personId").isJsonNull()) {
                return json.get("personId").getAsInt();
            }
            if (json.has("person") && json.get("person").isJsonObject()) {
                var p = json.getAsJsonObject("person");
                if (p.has("id") && !p.get("id").isJsonNull()) {
                    return p.get("id").getAsInt();
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void close() {
        ((Stage) loginField.getScene().getWindow()).close();
    }
}