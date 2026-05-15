package com.bazylev.client.controllers.shared;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.bazylev.client.util.SceneManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     lastNameField;
    @FXML private TextField     firstNameField;
    @FXML private TextField     middleNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     loginField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordRepeatField;
    @FXML private Button        registerButton;
    @FXML private Label         errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void onBack(ActionEvent event) {
        SceneManager.switchTo("/com/bazylev/client/views/shared/login.fxml", "Вход в систему");
    }

    @FXML
    private void onRegister(ActionEvent event) {
        String lastName   = safe(lastNameField.getText());
        String firstName  = safe(firstNameField.getText());
        String middleName = safe(middleNameField.getText());
        String email      = safe(emailField.getText());
        String login      = safe(loginField.getText());
        String password   = passwordField.getText() == null ? "" : passwordField.getText();
        String repeat     = passwordRepeatField.getText() == null ? "" : passwordRepeatField.getText();

        if (lastName.isBlank() || firstName.isBlank()) {
            showError("Введите имя и фамилию");
            return;
        }
        if (login.isBlank()) {
            showError("Введите логин");
            return;
        }
        if (password.isBlank()) {
            showError("Введите пароль");
            return;
        }
        if (!password.equals(repeat)) {
            showError("Пароли не совпадают");
            passwordRepeatField.clear();
            return;
        }

        registerButton.setDisable(true);
        hideError();

        JsonObject data = new JsonObject();
        data.addProperty("lastName",  lastName);
        data.addProperty("firstName", firstName);
        if (!middleName.isBlank()) data.addProperty("middleName", middleName);
        if (!email.isBlank())      data.addProperty("email",      email);
        data.addProperty("login",    login);
        data.addProperty("password", password);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.REGISTER_STUDENT, data.toString()));

        Platform.runLater(() -> {
            registerButton.setDisable(false);
            if (resp.getStatus() == ResponseStatus.OK) {
                AlertUtil.showInfo("Готово", "Регистрация успешна. Теперь войдите в систему.");
                SceneManager.switchTo("/com/bazylev/client/views/shared/login.fxml", "Вход в систему");
            } else {
                String msg = resp.getMessage();
                showError(msg != null && !msg.isBlank() ? msg : "Не удалось зарегистрироваться");
            }
        });
    }

    private static String safe(String s) {
        return s == null ? "" : s.strip();
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
}