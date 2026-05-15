package com.bazylev.client.controllers.admin;

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

public class MainAdminController {

    @FXML private Label     userInfoLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnSchedule;
    @FXML private Button btnGroups;
    @FXML private Button btnStudents;
    @FXML private Button btnTeachers;
    @FXML private Button btnCourses;
    @FXML private Button btnPayments;
    @FXML private Button btnDebtors;
    @FXML private Button btnReports;
    @FXML private Button btnUsers;

    private Button activeButton;

    private static final Map<String, String> FXML_MAP = Map.of(
            "dashboard", "/com/bazylev/client/views/admin/dashboard.fxml",
            "schedule",  "/com/bazylev/client/views/admin/schedule.fxml",
            "groups",    "/com/bazylev/client/views/admin/groups.fxml",
            "students",  "/com/bazylev/client/views/admin/students.fxml",
            "teachers",  "/com/bazylev/client/views/admin/teachers.fxml",
            "courses",   "/com/bazylev/client/views/admin/courses.fxml",
            "payments",  "/com/bazylev/client/views/admin/payments.fxml",
            "debtors",   "/com/bazylev/client/views/admin/debtors.fxml",
            "reports",   "/com/bazylev/client/views/admin/reports.fxml",
            "users",     "/com/bazylev/client/views/admin/users.fxml"
    );

    @FXML
    public void initialize() {
        userInfoLabel.setText(ClientSession.getInstance().getLogin());
        loadContent("dashboard", btnDashboard);
    }

    @FXML
    private void onNavClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String section = (String) clicked.getUserData();
        loadContent(section, clicked);
    }

    @FXML
    private void onLogout() {
        boolean confirmed = AlertUtil.showConfirm("Выход", "Вы действительно хотите выйти?");
        if (!confirmed) return;

        ClientSession.getInstance().clear();
        ServerConnection.getInstance().disconnect();

        try {
            ServerConnection.getInstance().connect();
        } catch (IOException e) {
            // Переходим на экран логина в любом случае
        }

        SceneManager.switchTo(
                "/com/bazylev/client/views/shared/login.fxml",
                "Вход в систему");
    }

    private void loadContent(String section, Button button) {
        String fxmlPath = FXML_MAP.get(section);
        if (fxmlPath == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().setAll(content);

            if (activeButton != null) {
                activeButton.getStyleClass().remove("nav-button-active");
            }
            button.getStyleClass().add("nav-button-active");
            activeButton = button;

        } catch (IOException e) {
            AlertUtil.showError("Ошибка навигации",
                    "Не удалось загрузить раздел: " + section + "\n" + e.getMessage());
        }
    }
}
