package com.bazylev.client.controllers.student;

import com.bazylev.client.enums.AttendanceStatus;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Attendance;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentAttendanceController {

    @FXML private TableView<Attendance>           table;
    @FXML private TableColumn<Attendance, String> colDate;
    @FXML private TableColumn<Attendance, String> colStatus;
    @FXML private TableColumn<Attendance, String> colComment;
    @FXML private Label                           statsLabel;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Attendance> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLessonDate() != null
                        ? d.getValue().getLessonDate().toString() : ""));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(formatStatus(d.getValue().getStatus())));
        colComment.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getComment() != null
                        ? d.getValue().getComment() : ""));

        table.setItems(items);
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
                .send(RequestBuilder.ofRaw(RequestType.GET_ATTENDANCE, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Attendance> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Attendance>>(){}.getType());
            list = list.stream()
                    .sorted((a, b) -> {
                        if (a.getLessonDate() == null) return 1;
                        if (b.getLessonDate() == null) return -1;
                        return b.getLessonDate().compareTo(a.getLessonDate());
                    })
                    .toList();
            items.setAll(list);
            updateStats(list);
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void updateStats(List<Attendance> list) {
        if (list.isEmpty()) {
            statsLabel.setText("Занятий не найдено");
            return;
        }

        Map<AttendanceStatus, Long> counts = list.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(
                        Attendance::getStatus, Collectors.counting()));

        long present = counts.getOrDefault(AttendanceStatus.PRESENT, 0L);
        long late    = counts.getOrDefault(AttendanceStatus.LATE,    0L);
        long absent  = counts.getOrDefault(AttendanceStatus.ABSENT,  0L);
        long excused = counts.getOrDefault(AttendanceStatus.EXCUSED, 0L);
        long total   = list.size();

        long attended  = present + late;
        double percent = total > 0 ? (double) attended / total * 100.0 : 0.0;

        statsLabel.setText(String.format(
                "Всего занятий: %d  |  ✅ Присутствовал: %d  |  🕐 Опоздал: %d  "
                        + "|  ❌ Отсутствовал: %d  |  📋 Уважительная: %d  "
                        + "|  📊 Посещаемость: %.1f%%",
                total, present, late, absent, excused, percent));

        if (percent >= 80) {
            statsLabel.setStyle("-fx-text-fill: #276749;");
        } else if (percent >= 60) {
            statsLabel.setStyle("-fx-text-fill: #C05621;");
        } else {
            statsLabel.setStyle("-fx-text-fill: #C53030;");
        }
    }

    private String formatStatus(AttendanceStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PRESENT -> "✅ Присутствовал";
            case LATE    -> "🕐 Опоздал";
            case ABSENT  -> "❌ Отсутствовал";
            case EXCUSED -> "📋 По уважительной";
        };
    }
}
