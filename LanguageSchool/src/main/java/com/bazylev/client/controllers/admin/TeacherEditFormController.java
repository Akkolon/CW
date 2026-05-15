package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.Teacher;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class TeacherEditFormController {

    @FXML private TextField     lastNameField;
    @FXML private TextField     firstNameField;
    @FXML private TextField     middleNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     specializationField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordRepeatField;
    @FXML private Label         errorLabel;

    private final Gson gson = GsonFactory.getInstance();
    private Teacher  editingTeacher;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    public void setTeacher(Teacher teacher) {
        this.editingTeacher = teacher;
        specializationField.setText(
                teacher.getSpecialization() != null
                        ? teacher.getSpecialization() : "");
        loadPersonData(teacher.getPersonId());
    }

    private void loadPersonData(int personId) {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_PERSONS));
        if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) return;

        List<Person> persons = gson.fromJson(resp.getData(),
                new TypeToken<List<Person>>(){}.getType());

        persons.stream()
                .filter(p -> p.getId() == personId)
                .findFirst()
                .ifPresent(p -> {
                    lastNameField.setText(
                            p.getLastName()   != null ? p.getLastName()   : "");
                    firstNameField.setText(
                            p.getFirstName()  != null ? p.getFirstName()  : "");
                    middleNameField.setText(
                            p.getMiddleName() != null ? p.getMiddleName() : "");
                    emailField.setText(
                            p.getEmail()      != null ? p.getEmail()       : "");
                });
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        JsonObject data = new JsonObject();
        data.addProperty("id",             editingTeacher.getId());
        data.addProperty("specialization", specializationField.getText().strip());
        data.addProperty("firstName",      firstNameField.getText().strip());
        data.addProperty("lastName",       lastNameField.getText().strip());
        data.addProperty("middleName",     middleNameField.getText().strip());
        data.addProperty("email",          emailField.getText().strip());

        String password = passwordField.getText();
        if (!password.isBlank()) {
            data.addProperty("password", password);
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.UPDATE_TEACHER, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            AlertUtil.showInfo("Успех", "Данные преподавателя обновлены");
            if (onSaved != null) onSaved.run();
            close();
        } else {
            showError(resp.getMessage() != null && !resp.getMessage().isBlank()
                    ? resp.getMessage() : "Не удалось сохранить изменения");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (lastNameField.getText().isBlank() || firstNameField.getText().isBlank()) {
            showError("Имя и фамилия не могут быть пустыми");
            return false;
        }
        String password = passwordField.getText();
        if (!password.isBlank()) {
            if (password.length() < 4) {
                showError("Новый пароль должен содержать не менее 4 символов");
                return false;
            }
            if (!password.equals(passwordRepeatField.getText())) {
                showError("Пароли не совпадают");
                passwordRepeatField.clear();
                return false;
            }
        }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) lastNameField.getScene().getWindow()).close();
    }
}