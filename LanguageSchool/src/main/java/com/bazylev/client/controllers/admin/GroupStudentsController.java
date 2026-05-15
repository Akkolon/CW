package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class GroupStudentsController {

    @FXML private Label                         titleLabel;
    @FXML private TableView<JsonObject>         table;
    @FXML private TableColumn<JsonObject, String> colId;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colDate;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private Label                         countLabel;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<JsonObject> students = FXCollections.observableArrayList();

    private Group group;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().has("studentId")
                                ? String.valueOf(d.getValue().get("studentId").getAsInt())
                                : ""));
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().has("fullName")
                                ? d.getValue().get("fullName").getAsString()
                                : "—"));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().has("enrollmentDate")
                                && !d.getValue().get("enrollmentDate").isJsonNull()
                                ? d.getValue().get("enrollmentDate").getAsString()
                                : "—"));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty("✅ Активен"));

        table.setItems(students);
    }

    public void setGroup(Group group) {
        this.group = group;
        titleLabel.setText("Студенты группы «" + group.getName() + "»");
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    @FXML
    private void onClose() {
        ((Stage) table.getScene().getWindow()).close();
    }

    private void load() {
        if (group == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("groupId", group.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_STUDENTS_BY_GROUP, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            JsonArray arr = JsonParser.parseString(resp.getData()).getAsJsonArray();
            students.clear();
            for (JsonElement el : arr) {
                students.add(el.getAsJsonObject());
            }
            countLabel.setText("Всего: " + students.size());
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }
}
