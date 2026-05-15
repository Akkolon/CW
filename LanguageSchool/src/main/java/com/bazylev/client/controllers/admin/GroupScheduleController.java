package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.DayOfWeek;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Schedule;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class GroupScheduleController {

    @FXML private Label                          titleLabel;
    @FXML private TableView<Schedule>            table;
    @FXML private TableColumn<Schedule, String>  colDay;
    @FXML private TableColumn<Schedule, String>  colStart;
    @FXML private TableColumn<Schedule, String>  colEnd;
    @FXML private TableColumn<Schedule, String>  colRoom;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Schedule> slots = FXCollections.observableArrayList();

    private Group group;

    @FXML
    public void initialize() {
        colDay.setCellValueFactory(d ->
                new SimpleStringProperty(formatDay(d.getValue().getDayOfWeek())));
        colStart.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStartTime() != null
                        ? d.getValue().getStartTime().toString() : "—"));
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEndTime() != null
                        ? d.getValue().getEndTime().toString() : "—"));
        colRoom.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRoom() != null
                        ? d.getValue().getRoom() : "—"));

        table.setItems(slots);
    }

    public void setGroup(Group group) {
        this.group = group;
        titleLabel.setText("Расписание группы «" + group.getName() + "»");
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
                .send(RequestBuilder.ofRaw(RequestType.GET_SCHEDULE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Schedule> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Schedule>>(){}.getType());
            list.sort(Comparator.comparingInt(s -> dayOrder(s.getDayOfWeek())));
            slots.setAll(list);
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private int dayOrder(DayOfWeek day) {
        if (day == null) return 99;
        return switch (day) {
            case MON -> 0;
            case TUE -> 1;
            case WED -> 2;
            case THU -> 3;
            case FRI -> 4;
            case SAT -> 5;
            case SUN -> 6;
        };
    }

    private String formatDay(DayOfWeek day) {
        if (day == null) return "—";
        return switch (day) {
            case MON -> "Понедельник";
            case TUE -> "Вторник";
            case WED -> "Среда";
            case THU -> "Четверг";
            case FRI -> "Пятница";
            case SAT -> "Суббота";
            case SUN -> "Воскресенье";
        };
    }
}