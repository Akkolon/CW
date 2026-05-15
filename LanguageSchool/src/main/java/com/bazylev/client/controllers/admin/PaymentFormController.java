package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.GroupStudentInfo;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentFormController {

    @FXML private ComboBox<GroupItem>   groupCombo;
    @FXML private ComboBox<StudentItem> studentCombo;
    @FXML private TextField             amountField;
    @FXML private DatePicker            datePicker;
    @FXML private ComboBox<String>      methodCombo;
    @FXML private TextField             receiptField;

    private final Gson gson = GsonFactory.getInstance();
    private Runnable onSaved;

    @FXML
    public void initialize() {
        methodCombo.setItems(FXCollections.observableArrayList("CASH", "CARD", "TRANSFER"));
        methodCombo.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());
        studentCombo.setDisable(true);
        loadGroups();
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    private void loadGroups() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) return;

        List<Group> list = gson.fromJson(resp.getData(),
                new TypeToken<List<Group>>(){}.getType());
        List<GroupItem> items = list.stream()
                .map(g -> new GroupItem(g.getId(), g.getName()))
                .toList();
        groupCombo.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    private void onGroupSelected() {
        GroupItem selected = groupCombo.getValue();
        if (selected == null) {
            studentCombo.setItems(FXCollections.observableArrayList());
            studentCombo.setDisable(true);
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("groupId", selected.id());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_STUDENTS_BY_GROUP, data.toString()));

        List<StudentItem> items = new ArrayList<>();
        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            List<GroupStudentInfo> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<GroupStudentInfo>>(){}.getType());
            for (GroupStudentInfo gs : list) {
                items.add(new StudentItem(gs.getStudentId(), gs.getFullName()));
            }
        }

        studentCombo.setItems(FXCollections.observableArrayList(items));
        studentCombo.setDisable(items.isEmpty());
        studentCombo.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        StudentItem student = studentCombo.getValue();

        JsonObject data = new JsonObject();
        data.addProperty("studentId",     student.studentId());
        data.addProperty("amount",        new BigDecimal(amountField.getText().strip()));
        data.addProperty("paymentDate",   datePicker.getValue().toString());
        data.addProperty("paymentMethod", methodCombo.getValue());
        if (!receiptField.getText().isBlank()) {
            data.addProperty("receiptNumber", receiptField.getText().strip());
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.REGISTER_PAYMENT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            AlertUtil.showInfo("Успех", "Платёж успешно зарегистрирован");
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
        if (groupCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите группу");
            return false;
        }
        if (studentCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите студента");
            return false;
        }

        String amt = amountField.getText().strip();
        if (amt.isBlank()) {
            AlertUtil.showWarning("Валидация", "Введите сумму платежа");
            return false;
        }
        try {
            BigDecimal amount = new BigDecimal(amt);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtil.showWarning("Валидация", "Сумма должна быть больше нуля");
                return false;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Валидация", "Некорректный формат суммы");
            return false;
        }

        if (datePicker.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите дату платежа");
            return false;
        }
        return true;
    }

    private void close() {
        ((Stage) amountField.getScene().getWindow()).close();
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    record StudentItem(int studentId, String name) {
        @Override public String toString() { return name; }
    }
}
