package com.bazylev.client.controllers.student;

import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.session.ClientSession;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Map;

public class MainStudentController {

    @FXML private Label     userInfoLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnSchedule;
    @FXML private Button btnGrades;
    @FXML private Button btnAttendance;
    @FXML private Button btnPayments;

    private Button activeButton;

    private static final Map<String, String> FXML_MAP = Map.of(
            "schedule",   "/com/bazylev/client/views/student/schedule.fxml",
            "grades",     "/com/bazylev/client/views/student/grades.fxml",
            "attendance", "/com/bazylev/client/views/student/attendance.fxml",
            "payments",   "/com/bazylev/client/views/student/payments.fxml"
    );

    @FXML
    public void initialize() {
        userInfoLabel.setText(ClientSession.getInstance().getLogin());
        loadContent("schedule", btnSchedule);
    }

    @FXML
    private void onNavClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String section = (String) clicked.getUserData();
        loadContent(section, clicked);
    }

    @FXML
    private void onLogout() {
        if (!AlertUtil.showConfirm("Выход", "Вы действительно хотите выйти?")) return;
        ClientSession.getInstance().clear();
        ServerConnection.getInstance().disconnect();
        try { ServerConnection.getInstance().connect(); } catch (IOException ignored) {}
        SceneManager.switchTo("/com/bazylev/client/views/shared/login.fxml", "Вход в систему");
    }

    private void loadContent(String section, Button button) {
        String path = FXML_MAP.get(section);
        if (path == null) return;
        try {
            Node content = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(content);
            if (activeButton != null) activeButton.getStyleClass().remove("nav-button-active");
            button.getStyleClass().add("nav-button-active");
            activeButton = button;
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось загрузить раздел: " + e.getMessage());
        }
    }
}
