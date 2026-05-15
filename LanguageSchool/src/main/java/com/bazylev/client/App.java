package com.bazylev.client;

import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Школа иностранных языков");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);

        try {
            ServerConnection.getInstance().connect();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка подключения",
                    "Не удалось подключиться к серверу.\n" +
                    "Убедитесь, что сервер запущен, и повторите попытку.\n\n" +
                    "Детали: " + e.getMessage());
        }

        SceneManager.switchTo(
                "/com/bazylev/client/views/shared/login.fxml",
                "Вход в систему");

        primaryStage.setOnCloseRequest(event -> {
            ServerConnection.getInstance().disconnect();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
