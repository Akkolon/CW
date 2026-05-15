package com.bazylev.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class SceneManager {

    private static Stage primaryStage;
    private static final String GLOBAL_STYLESHEET = "/com/bazylev/client/css/styles.css";

    private SceneManager() {}

    public static URL resolveUrl(String resourcePath) {
        URL url = SceneManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return url;
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyGlobalStyles(scene);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка навигации",
                    "Не удалось загрузить экран: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    public static void switchTo(String fxmlPath, String title) {
        switchTo(fxmlPath);
        primaryStage.setTitle(title);
    }

    public static <T> T switchToAndGetController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        applyGlobalStyles(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        return loader.getController();
    }

    public static void openModal(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage modal = new Stage();
            modal.setTitle(title);
            Scene scene = new Scene(root);
            applyGlobalStyles(scene);
            modal.setScene(scene);
            modal.initModality(Modality.WINDOW_MODAL);
            modal.initOwner(primaryStage);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть окно: " + e.getMessage());
        }
    }

    public static <T> T openModalAndGetController(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Stage modal = new Stage();
        modal.setTitle(title);
        Scene scene = new Scene(root);
        applyGlobalStyles(scene);
        modal.setScene(scene);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(primaryStage);
        modal.showAndWait();
        return loader.getController();
    }

    private static void applyGlobalStyles(Scene scene) {
        URL css = SceneManager.class.getResource(GLOBAL_STYLESHEET);
        if (css == null) return;
        String cssUrl = css.toExternalForm();
        if (!scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
        }
    }
}
