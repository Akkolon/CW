package com.bazylev.client.controllers.student;

import com.bazylev.client.enums.PaymentMethod;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Payment;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.session.ClientSession;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;

public class StudentPaymentsController {

    @FXML private TableView<Payment>             table;
    @FXML private TableColumn<Payment, String>   colAmount;
    @FXML private TableColumn<Payment, String>   colDate;
    @FXML private TableColumn<Payment, String>   colMethod;
    @FXML private TableColumn<Payment, String>   colReceipt;
    @FXML private Label                          totalLabel;
    @FXML private Label                          debtLabel;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmount() != null
                        ? d.getValue().getAmount().toPlainString() + " руб." : ""));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPaymentDate() != null
                        ? d.getValue().getPaymentDate().toString() : ""));
        colMethod.setCellValueFactory(d ->
                new SimpleStringProperty(formatMethod(d.getValue().getPaymentMethod())));
        colReceipt.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getReceiptNumber() != null
                        ? d.getValue().getReceiptNumber() : ""));

        table.setItems(payments);
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void load() {
        // Используем studentId, а не userId
        int studentId = ClientSession.getInstance().getStudentId();
        if (studentId <= 0) {
            AlertUtil.showError("Ошибка", "Не определён ID студента. Попробуйте войти заново.");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("studentId", studentId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_PAYMENTS, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Payment> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Payment>>(){}.getType());
            payments.setAll(list);

            BigDecimal total = list.stream()
                    .filter(p -> p.getAmount() != null)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalLabel.setText("Оплачено: " + total.toPlainString() + " руб.");
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }

        loadDebt(studentId);
    }

    private void loadDebt(int studentId) {
        JsonObject data = new JsonObject();
        data.addProperty("studentId", studentId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_DEBT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            JsonObject result = JsonParser.parseString(resp.getData()).getAsJsonObject();
            BigDecimal debt = result.get("debt").getAsBigDecimal();

            if (debt.compareTo(BigDecimal.ZERO) > 0) {
                debtLabel.setText("Задолженность: " + debt.toPlainString() + " руб.");
                debtLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
            } else {
                debtLabel.setText("Задолженности нет ✅");
                debtLabel.setStyle("-fx-text-fill: #276749; -fx-font-weight: bold;");
            }
        }
    }

    private String formatMethod(PaymentMethod method) {
        if (method == null) return "";
        return switch (method) {
            case CASH     -> "Наличные";
            case CARD     -> "Карта";
            case TRANSFER -> "Перевод";
        };
    }
}
