package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.DayOfWeek;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Schedule;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScheduleSlotFormController {

    @FXML private ComboBox<String> dayCombo;
    @FXML private TextField        startField;
    @FXML private TextField        endField;
    @FXML private TextField        roomField;
    @FXML private Label            groupLabel;
    @FXML private Label            errorLabel;

    private static final Map<String, String> DAY_MAP = new LinkedHashMap<>();
    static {
        DAY_MAP.put("Понедельник", "MON");
        DAY_MAP.put("Вторник",     "TUE");
        DAY_MAP.put("Среда",       "WED");
        DAY_MAP.put("Четверг",     "THU");
        DAY_MAP.put("Пятница",     "FRI");
        DAY_MAP.put("Суббота",     "SAT");
        DAY_MAP.put("Воскресенье", "SUN");
    }

    private static final Map<String, String> DAY_MAP_REVERSE = new LinkedHashMap<>();
    static {
        DAY_MAP.forEach((k, v) -> DAY_MAP_REVERSE.put(v, k));
    }

    private Schedule slot;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        dayCombo.setItems(FXCollections.observableArrayList(DAY_MAP.keySet()));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setSlot(Schedule slot) {
        this.slot = slot;

        if (slot.getDayOfWeek() != null) {
            String readable = DAY_MAP_REVERSE.getOrDefault(
                    slot.getDayOfWeek().name(), "Понедельник");
            dayCombo.setValue(readable);
        } else {
            dayCombo.getSelectionModel().selectFirst();
        }

        startField.setText(slot.getStartTime() != null
                ? slot.getStartTime().toString() : "");
        endField.setText(slot.getEndTime() != null
                ? slot.getEndTime().toString() : "");
        roomField.setText(slot.getRoom() != null
                ? slot.getRoom() : "");

        if (groupLabel != null) {
            groupLabel.setText("Группа #" + slot.getGroupId());
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        String dayEnum = DAY_MAP.get(dayCombo.getValue());

        JsonObject data = new JsonObject();
        data.addProperty("id",        slot.getId());
        data.addProperty("dayOfWeek", dayEnum);
        data.addProperty("startTime", startField.getText().strip());
        data.addProperty("endTime",   endField.getText().strip());
        data.addProperty("room",      roomField.getText().strip());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.UPDATE_SCHEDULE_SLOT, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            if (onSaved != null) onSaved.run();
            close();
        } else {
            showError(resp.getMessage() != null && !resp.getMessage().isBlank()
                    ? resp.getMessage() : "Не удалось сохранить слот");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (dayCombo.getValue() == null) {
            showError("Выберите день недели");
            return false;
        }

        LocalTime start;
        LocalTime end;

        try {
            start = LocalTime.parse(startField.getText().strip());
        } catch (DateTimeParseException e) {
            showError("Некорректный формат времени начала (пример: 08:00)");
            return false;
        }
        try {
            end = LocalTime.parse(endField.getText().strip());
        } catch (DateTimeParseException e) {
            showError("Некорректный формат времени конца (пример: 09:30)");
            return false;
        }

        if (!end.isAfter(start)) {
            showError("Время конца должно быть позже времени начала");
            return false;
        }

        if (roomField.getText().isBlank()) {
            showError("Введите номер аудитории");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) dayCombo.getScene().getWindow()).close();
    }
}