package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DebtorsController {

    @FXML private TableView<JsonObject>           table;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colCourse;
    @FXML private TableColumn<JsonObject, String> colDebt;
    @FXML private Label                           countLabel;

    private final ObservableList<JsonObject> debtors = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("fullName").getAsString()));
        colCourse.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("courseName").getAsString()));
        colDebt.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("debt").getAsBigDecimal()
                                .toPlainString() + " руб."));

        table.setItems(debtors);
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void load() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_DEBTORS));

        if (resp.getStatus() == ResponseStatus.OK) {
            debtors.clear();
            JsonArray arr = JsonParser.parseString(resp.getData()).getAsJsonArray();
            for (JsonElement el : arr) {
                debtors.add(el.getAsJsonObject());
            }
            countLabel.setText("Всего должников: " + debtors.size());
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }
}