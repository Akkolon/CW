package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.PaymentMethod;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.GroupStudentInfo;
import com.bazylev.client.models.entities.Payment;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentsController {

    @FXML private ComboBox<GroupItem>   groupCombo;
    @FXML private ComboBox<StudentItem> studentCombo;
    @FXML private Label                 debtLabel;
    @FXML private Label                 countLabel;
    @FXML private Label                 totalLabel;

    @FXML private TableView<Payment>             table;
    @FXML private TableColumn<Payment, Integer>  colId;
    @FXML private TableColumn<Payment, Integer>  colStudent;
    @FXML private TableColumn<Payment, String>   colAmount;
    @FXML private TableColumn<Payment, String>   colDate;
    @FXML private TableColumn<Payment, String>   colMethod;
    @FXML private TableColumn<Payment, String>   colReceipt;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colStudent.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStudentId()).asObject());
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmount() != null
                        ? d.getValue().getAmount().toPlainString() + " руб." : ""));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPaymentDate() != null
                        ? d.getValue().getPaymentDate().toString() : ""));
        colMethod.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPaymentMethod() != null
                        ? formatMethod(d.getValue().getPaymentMethod()) : ""));
        colReceipt.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getReceiptNumber() != null
                        ? d.getValue().getReceiptNumber() : ""));
        table.setItems(payments);

        studentCombo.setDisable(true);
        loadGroups();
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
        loadStudentsForGroup(selected.id());
    }

    private void loadStudentsForGroup(int groupId) {
        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupId);

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
        payments.clear();
        debtLabel.setText("");
        countLabel.setText("Всего: 0");
        totalLabel.setText("Итого: 0.00");
    }

    @FXML
    private void onSearch() {
        StudentItem student = studentCombo.getValue();
        if (student == null) {
            AlertUtil.showWarning("Фильтр", "Выберите студента");
            return;
        }
        loadByStudent(student.studentId());
        loadDebt(student.studentId());
    }

    private void loadByStudent(int studentId) {
        JsonObject data = new JsonObject();
        data.addProperty("studentId", studentId);
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_PAYMENTS, data.toString()));
        handleResponse(resp);
    }

    private void loadDebt(int studentId) {
        JsonObject data = new JsonObject();
        data.addProperty("studentId", studentId);
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_DEBT, data.toString()));
        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            JsonObject result = JsonParser.parseString(resp.getData()).getAsJsonObject();
            BigDecimal debt = result.get("debt").getAsBigDecimal();
            debtLabel.setText("Задолженность: " + debt.toPlainString() + " руб.");
            debtLabel.setStyle(debt.compareTo(BigDecimal.ZERO) > 0
                    ? "-fx-text-fill: #E53E3E; -fx-font-weight: bold;"
                    : "-fx-text-fill: #276749; -fx-font-weight: bold;");
        }
    }

    private void handleResponse(Response resp) {
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Payment> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Payment>>(){}.getType());
            payments.setAll(list);
            BigDecimal total = list.stream()
                    .filter(p -> p.getAmount() != null)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            countLabel.setText("Всего: " + list.size());
            totalLabel.setText("Итого: " + total.toPlainString() + " руб.");
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onReset() {
        groupCombo.getSelectionModel().clearSelection();
        studentCombo.setItems(FXCollections.observableArrayList());
        studentCombo.setDisable(true);
        debtLabel.setText("");
        payments.clear();
        countLabel.setText("Всего: 0");
        totalLabel.setText("Итого: 0.00");
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/payment-form.fxml"));
            javafx.scene.Parent root = loader.load();
            PaymentFormController ctrl = loader.getController();
            ctrl.setOnSaved(this::onSearch);

            Stage modal = new Stage();
            modal.setTitle("Зарегистрировать платёж");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    private String formatMethod(PaymentMethod method) {
        return switch (method) {
            case CASH     -> "Наличные";
            case CARD     -> "Карта";
            case TRANSFER -> "Перевод";
        };
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    record StudentItem(int studentId, String name) {
        @Override public String toString() { return name; }
    }
}
