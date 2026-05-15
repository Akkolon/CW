package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class StudentFormController {

    @FXML private TextField     lastNameField;
    @FXML private TextField     firstNameField;
    @FXML private TextField     middleNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     loginField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordRepeatField;
    @FXML private Label         errorLabel;

    private Runnable onSaved;

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        JsonObject data = new JsonObject();
        data.addProperty("lastName",  lastNameField.getText().strip());
        data.addProperty("firstName", firstNameField.getText().strip());

        String middle = middleNameField.getText().strip();
        if (!middle.isBlank()) data.addProperty("middleName", middle);

        String email = emailField.getText().strip();
        if (!email.isBlank()) data.addProperty("email", email);

        data.addProperty("login",    loginField.getText().strip());
        data.addProperty("password", passwordField.getText());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.REGISTER_STUDENT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            AlertUtil.showInfo("Успех", "Студент успешно добавлен");
            if (onSaved != null) onSaved.run();
            close();
        } else {
            showError(resp.getMessage() != null && !resp.getMessage().isBlank()
                    ? resp.getMessage()
                    : "Не удалось создать студента");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
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
        if (!passwordField.getText().equals(passwordRepeatField.getText())) {
            showError("Пароли не совпадают");
            passwordRepeatField.clear();
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
        ((Stage) loginField.getScene().getWindow()).close();
    }
}