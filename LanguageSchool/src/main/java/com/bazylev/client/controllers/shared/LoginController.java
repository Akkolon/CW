package com.bazylev.client.controllers.shared;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.enums.Role;
import com.bazylev.client.models.tcp.Request;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.session.ClientSession;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.bazylev.client.util.SceneManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class LoginController {

    @FXML private TextField     loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });
        loginField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });
    }

    @FXML
    private void onLoginButtonPressed(ActionEvent event) {
        handleLogin();
    }

    @FXML
    private void onRegisterButtonPressed(ActionEvent event) {
        SceneManager.switchTo(
                "/com/bazylev/client/views/shared/register.fxml",
                "Регистрация (студент)");
    }

    private void handleLogin() {
        String login    = loginField.getText().strip();
        String password = passwordField.getText();

        if (login.isBlank() || password.isBlank()) {
            showError("Введите логин и пароль");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        JsonObject data = new JsonObject();
        data.addProperty("login",    login);
        data.addProperty("password", password);

        Request request = RequestBuilder.ofRaw(RequestType.LOGIN, data.toString());
        Response response = ServerConnection.getInstance().send(request);

        Platform.runLater(() -> {
            loginButton.setDisable(false);

            if (response.getStatus() == ResponseStatus.OK) {
                processSuccessfulLogin(response.getData());
            } else {
                String msg = response.getMessage();
                if (msg == null || msg.isBlank()) {
                    msg = switch (response.getStatus()) {
                        case UNAUTHORIZED -> "Неверный логин или пароль";
                        case FORBIDDEN    -> "Доступ запрещён (возможно, аккаунт заблокирован)";
                        case ERROR        -> "Ошибка на сервере";
                        default           -> "Не удалось войти: " + response.getStatus();
                    };
                }
                showError(msg);
                passwordField.clear();
            }
        });
    }

    private void processSuccessfulLogin(String responseData) {
        try {
            if (responseData == null || responseData.isBlank()) {
                showError("Сервер вернул пустой ответ (data) при успешном входе");
                return;
            }

            JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();

            String token = firstNonBlank(
                    getAsString(json, "token"),
                    getAsString(json, "accessToken"),
                    getAsString(json, "jwt"));

            String roleRaw = firstNonBlank(
                    getAsString(json, "role"),
                    getAsString(json, "userRole"));

            String loginStr = firstNonBlank(
                    getAsString(json, "login"),
                    getAsString(json, "username"),
                    getAsString(json, "email"));

            Integer userId = firstNonNull(
                    getAsInt(json, "userId"),
                    getAsInt(json, "id"));

            if (token == null || roleRaw == null || loginStr == null || userId == null) {
                showError("Неожиданный формат ответа сервера при входе: " + responseData);
                return;
            }

            Role role = parseRole(roleRaw);

            // Для студентов читаем studentId из ответа
            Integer studentId = getAsInt(json, "studentId");

            if (role == Role.STUDENT && (studentId == null || studentId <= 0)) {
                // studentId не пришёл — логиним с userId=0 для studentId,
                // но показываем ошибку чтобы было понятно
                showError("Не удалось определить ID студента. Обратитесь к администратору.");
                return;
            }

            if (role == Role.STUDENT) {
                ClientSession.getInstance().init(token, role, loginStr, userId, studentId);
            } else {
                ClientSession.getInstance().init(token, role, loginStr, userId);
            }

            navigateByRole(role);
        } catch (Exception e) {
            showError("Ошибка обработки ответа сервера: " + e.getMessage());
        }
    }

    private static Role parseRole(String raw) {
        String r = raw.strip().toUpperCase();
        return switch (r) {
            case "ADMIN", "ADMINISTRATOR" -> Role.ADMIN;
            case "TEACHER", "TUTOR"       -> Role.TEACHER;
            case "STUDENT", "LEARNER"     -> Role.STUDENT;
            default -> Role.valueOf(r);
        };
    }

    private static String getAsString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        if (json.get(key) instanceof JsonPrimitive p && p.isString()) return p.getAsString();
        return String.valueOf(json.get(key)).replace("\"", "");
    }

    private static Integer getAsInt(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        try {
            return json.get(key).getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static Integer firstNonNull(Integer... values) {
        if (values == null) return null;
        for (Integer v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private void navigateByRole(Role role) {
        switch (role) {
            case ADMIN   -> SceneManager.switchTo(
                    "/com/bazylev/client/views/admin/main-admin.fxml",
                    "Администратор — Школа иностранных языков");
            case TEACHER -> SceneManager.switchTo(
                    "/com/bazylev/client/views/teacher/main-teacher.fxml",
                    "Преподаватель — Школа иностранных языков");
            case STUDENT -> SceneManager.switchTo(
                    "/com/bazylev/client/views/student/main-student.fxml",
                    "Учащийся — Школа иностранных языков");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
