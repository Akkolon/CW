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
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleController {

    @FXML private ComboBox<GroupItem> groupCombo;
    @FXML private Label               statusLabel;
    @FXML private Label               countLabel;

    @FXML private TableView<Schedule>            table;
    @FXML private TableColumn<Schedule, String>  colDay;
    @FXML private TableColumn<Schedule, String>  colStart;
    @FXML private TableColumn<Schedule, String>  colEnd;
    @FXML private TableColumn<Schedule, String>  colRoom;
    @FXML private TableColumn<Schedule, String>  colGroup;

    @FXML private Label statTotalLabel;
    @FXML private Label statUsedLabel;
    @FXML private Label statFreeLabel;
    @FXML private Label statGroupsLabel;
    @FXML private Label statWithLabel;
    @FXML private Label statWithoutLabel;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Schedule> slots = FXCollections.observableArrayList();
    private final Map<Integer, String> groupNameById = new HashMap<>();

    @FXML
    public void initialize() {
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
        colGroup.setCellValueFactory(d ->
                new SimpleStringProperty(
                        groupNameById.getOrDefault(
                                d.getValue().getGroupId(),
                                "Группа #" + d.getValue().getGroupId())));

        table.setItems(slots);
        loadGroups();
        loadStats();
    }

    private void loadGroups() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_GROUPS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<Group> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Group>>(){}.getType());
            groupNameById.clear();
            List<GroupItem> items = list.stream()
                    .map(g -> {
                        groupNameById.put(g.getId(), g.getName());
                        return new GroupItem(g.getId(), g.getName());
                    })
                    .toList();
            groupCombo.setItems(FXCollections.observableArrayList(items));
        }
    }

    private void loadStats() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_SCHEDULE_STATS));
        if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) return;

        JsonObject s = JsonParser.parseString(resp.getData()).getAsJsonObject();
        statTotalLabel.setText(String.valueOf(s.get("totalSlots").getAsInt()));
        statUsedLabel.setText(String.valueOf(s.get("usedSlots").getAsInt()));
        statFreeLabel.setText(String.valueOf(s.get("freeSlots").getAsInt()));
        statGroupsLabel.setText(String.valueOf(s.get("activeGroups").getAsInt()));
        statWithLabel.setText(String.valueOf(s.get("groupsWithSched").getAsInt()));
        statWithoutLabel.setText(String.valueOf(s.get("groupsWithout").getAsInt()));
    }

    @FXML private void onGroupSelected() { loadSlots(); }

    @FXML
    private void onRefresh() {
        loadGroups();
        loadSlots();
        loadStats();
    }

    private void loadSlots() {
        if (groupCombo.getValue() == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("groupId", groupCombo.getValue().id());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GET_SCHEDULE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Schedule> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Schedule>>(){}.getType());
            slots.setAll(list);
            countLabel.setText("Слотов: " + list.size());
            statusLabel.setText("");
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onGenerate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/generate-schedule-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            GenerateScheduleDialogController ctrl = loader.getController();

            Stage dialog = new Stage();
            dialog.setTitle("Генерация расписания");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.initModality(Modality.APPLICATION_MODAL);

            ctrl.setOnConfirmed(params -> {
                dialog.close();
                runGeneration(params.lessonsPerWeek(), params.overwrite());
            });

            dialog.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть диалог: " + e.getMessage());
        }
    }

    private void runGeneration(int lessonsPerWeek, boolean overwrite) {
        JsonObject params = new JsonObject();
        params.addProperty("lessonsPerWeek", lessonsPerWeek);
        params.addProperty("overwrite",      overwrite);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GENERATE_SCHEDULE, params.toString()));

        if (resp.getStatus() != ResponseStatus.OK) {
            AlertUtil.showError("Ошибка", resp.getMessage());
            return;
        }

        JsonObject result   = JsonParser.parseString(resp.getData()).getAsJsonObject();
        JsonArray succeeded = result.getAsJsonArray("succeeded");
        JsonArray errors    = result.getAsJsonArray("errors");
        JsonArray skipped   = result.getAsJsonArray("skipped");

        int succeededCount = succeeded.size();
        int errorsCount    = errors.size();
        int skippedCount   = skipped.size();

        statusLabel.setText("Готово: " + succeededCount
                + " групп, пропущено: " + skippedCount
                + ", ошибок: " + errorsCount);

        loadSlots();
        loadStats();

        StringBuilder sb = new StringBuilder();

        if (succeededCount > 0) {
            sb.append("✅ Успешно (").append(succeededCount).append("):\n");
            for (JsonElement e : succeeded) {
                sb.append("  • ").append(e.getAsString()).append("\n");
            }
            sb.append("\n");
        }

        if (skippedCount > 0) {
            sb.append("⏭ Пропущено (").append(skippedCount).append("):\n");
            for (JsonElement e : skipped) {
                sb.append("  • ").append(e.getAsString()).append("\n");
            }
            sb.append("\n");
        }

        if (errorsCount > 0) {
            sb.append("⚠ Не удалось полностью разместить (")
                    .append(errorsCount).append("):\n");
            for (JsonElement e : errors) {
                sb.append("  • ").append(e.getAsString()).append("\n");
            }
        }

        Alert alert = new Alert(errorsCount > 0
                ? Alert.AlertType.WARNING
                : Alert.AlertType.INFORMATION);
        alert.setTitle("Результат генерации");
        alert.setHeaderText("Занятий в неделю: " + lessonsPerWeek
                + " | Успешно: " + succeededCount
                + ", пропущено: " + skippedCount
                + ", ошибок: " + errorsCount);

        String bodyText = sb.isEmpty()
                ? "Нет активных групп для обработки."
                : sb.toString();

        TextArea area = new TextArea(bodyText);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(500, 320);
        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setMinWidth(540);
        alert.getDialogPane().setMinHeight(400);

        alert.showAndWait();
    }

    @FXML
    private void onDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            onEditSlot();
        }
    }

    @FXML
    private void onEditSlot() {
        Schedule selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите слот расписания для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/schedule-slot-form.fxml"));
            javafx.scene.Parent root = loader.load();
            ScheduleSlotFormController ctrl = loader.getController();
            ctrl.setSlot(selected);
            ctrl.setOnSaved(() -> {
                loadSlots();
                loadStats();
            });

            Stage modal = new Stage();
            modal.setTitle("Редактировать слот расписания");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteSlot() {
        Schedule selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Выбор", "Выберите слот расписания для удаления");
            return;
        }

        String day   = formatDay(selected.getDayOfWeek());
        String start = selected.getStartTime() != null ? selected.getStartTime().toString() : "?";
        String end   = selected.getEndTime()   != null ? selected.getEndTime().toString()   : "?";
        String room  = selected.getRoom()      != null ? selected.getRoom()                 : "?";

        if (!AlertUtil.showConfirm("Удаление слота",
                "Удалить слот расписания?\n\n"
                        + day + "  " + start + " – " + end
                        + "  (ауд. " + room + ")")) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("slotId", selected.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_SCHEDULE_SLOT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            loadSlots();
            loadStats();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
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

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
