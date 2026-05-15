package com.bazylev.client.controllers.teacher;

import com.bazylev.client.enums.DayOfWeek;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.Schedule;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.session.ClientSession;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class ScheduleController {

    @FXML private ComboBox<GroupItem>  groupCombo;
    @FXML private ComboBox<String>     dayCombo;
    @FXML private Label                countLabel;

    @FXML private TableView<Schedule>            table;
    @FXML private TableColumn<Schedule, String>  colGroup;
    @FXML private TableColumn<Schedule, String>  colDay;
    @FXML private TableColumn<Schedule, String>  colStart;
    @FXML private TableColumn<Schedule, String>  colEnd;
    @FXML private TableColumn<Schedule, String>  colRoom;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Schedule> all      = FXCollections.observableArrayList();
    private       FilteredList<Schedule>   filtered;

    @FXML
    public void initialize() {
        colGroup.setCellValueFactory(d ->
                new SimpleStringProperty(resolveGroupName(d.getValue().getGroupId())));
        colDay.setCellValueFactory(d ->
                new SimpleStringProperty(formatDay(d.getValue().getDayOfWeek())));
        colStart.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStartTime() != null
                        ? d.getValue().getStartTime().toString() : ""));
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEndTime() != null
                        ? d.getValue().getEndTime().toString() : ""));
        colRoom.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRoom() != null
                        ? d.getValue().getRoom() : ""));

        dayCombo.setItems(FXCollections.observableArrayList(
                "Все", "Понедельник", "Вторник", "Среда",
                "Четверг", "Пятница", "Суббота", "Воскресенье"));
        dayCombo.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(all, s -> true);
        table.setItems(filtered);

        loadGroups();
    }

    private void loadGroups() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Group> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            List<GroupItem> items = list.stream()
                    .map(g -> new GroupItem(g.getId(), g.getName()))
                    .toList();
            groupCombo.setItems(FXCollections.observableArrayList(items));
            if (!items.isEmpty()) {
                groupCombo.getSelectionModel().selectFirst();
                loadSlots();
            }
        }
    }

    @FXML
    private void onGroupSelected() { loadSlots(); }

    @FXML
    private void onDaySelected()   { applyDayFilter(); }

    @FXML
    private void onShowAll() {
        dayCombo.getSelectionModel().selectFirst();
        applyDayFilter();
    }

    @FXML
    private void onRefresh() { loadSlots(); }

    private void loadSlots() {
        if (groupCombo.getValue() == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupCombo.getValue().id());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_SCHEDULE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Schedule> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Schedule>>(){}.getType());
            all.setAll(list);
            applyDayFilter();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void applyDayFilter() {
        String sel = dayCombo.getValue();
        if (sel == null || sel.equals("Все")) {
            filtered.setPredicate(s -> true);
        } else {
            DayOfWeek target = parseDayName(sel);
            filtered.setPredicate(s -> s.getDayOfWeek() == target);
        }
        countLabel.setText("Слотов: " + filtered.size());
    }

    private String resolveGroupName(int groupId) {
        if (groupCombo.getItems() == null) return "Группа #" + groupId;
        return groupCombo.getItems().stream()
                .filter(gi -> gi.id() == groupId)
                .map(GroupItem::name)
                .findFirst()
                .orElse("Группа #" + groupId);
    }

    private String formatDay(DayOfWeek day) {
        if (day == null) return "";
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

    private DayOfWeek parseDayName(String name) {
        return switch (name) {
            case "Понедельник" -> DayOfWeek.MON;
            case "Вторник"     -> DayOfWeek.TUE;
            case "Среда"       -> DayOfWeek.WED;
            case "Четверг"     -> DayOfWeek.THU;
            case "Пятница"     -> DayOfWeek.FRI;
            case "Суббота"     -> DayOfWeek.SAT;
            case "Воскресенье" -> DayOfWeek.SUN;
            default            -> DayOfWeek.MON;
        };
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}