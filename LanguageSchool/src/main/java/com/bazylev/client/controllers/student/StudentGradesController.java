package com.bazylev.client.controllers.student;

import com.bazylev.client.enums.GradeType;
import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Grade;
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
import java.util.OptionalDouble;

public class StudentGradesController {

    @FXML private TableView<Grade>            table;
    @FXML private TableColumn<Grade, String>  colType;
    @FXML private TableColumn<Grade, String>  colValue;
    @FXML private TableColumn<Grade, String>  colDate;
    @FXML private Label                       countLabel;
    @FXML private Label                       averageLabel;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<Grade> grades = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(formatType(d.getValue().getGradeType())));
        colValue.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("%.1f", d.getValue().getValue())));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getGradeDate() != null
                        ? d.getValue().getGradeDate().toString() : ""));

        table.setItems(grades);
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
                .send(RequestBuilder.ofRaw(RequestType.GET_GRADES, data.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            List<Grade> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Grade>>(){}.getType());
            list = list.stream()
                    .sorted((a, b) -> {
                        if (a.getGradeDate() == null) return 1;
                        if (b.getGradeDate() == null) return -1;
                        return b.getGradeDate().compareTo(a.getGradeDate());
                    })
                    .toList();
            grades.setAll(list);
            updateStats();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void updateStats() {
        countLabel.setText("Всего оценок: " + grades.size());

        if (grades.isEmpty()) {
            averageLabel.setText("Средний балл: —");
            averageLabel.setStyle("");
            return;
        }

        OptionalDouble avg = grades.stream()
                .mapToDouble(Grade::getValue)
                .average();

        if (avg.isPresent()) {
            double val = avg.getAsDouble();
            averageLabel.setText(String.format("Средний балл: %.2f / 10.00", val));
            if (val >= 7.0) {
                averageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #276749;");
            } else if (val >= 5.0) {
                averageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #C05621;");
            } else {
                averageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #C53030;");
            }
        } else {
            averageLabel.setText("Средний балл: —");
        }
    }

    private String formatType(GradeType type) {
        if (type == null) return "";
        return switch (type) {
            case HOMEWORK -> "Домашнее задание";
            case TEST     -> "Тест";
            case EXAM     -> "Экзамен";
            case ACTIVITY -> "Активность";
        };
    }
}
