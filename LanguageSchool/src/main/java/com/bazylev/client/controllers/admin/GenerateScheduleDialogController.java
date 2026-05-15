package com.bazylev.client.controllers.admin;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class GenerateScheduleDialogController {

    @FXML private Spinner<Integer> lessonsSpinner;
    @FXML private CheckBox         overwriteCheck;

    private Consumer<GenerateParams> onConfirmed;

    public void setOnConfirmed(Consumer<GenerateParams> callback) {
        this.onConfirmed = callback;
    }

    @FXML
    private void onConfirm() {
        int lessons   = lessonsSpinner.getValue();
        boolean overwrite = overwriteCheck.isSelected();
        if (onConfirmed != null) {
            onConfirmed.accept(new GenerateParams(lessons, overwrite));
        }
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) lessonsSpinner.getScene().getWindow()).close();
    }

    public record GenerateParams(int lessonsPerWeek, boolean overwrite) {}
}