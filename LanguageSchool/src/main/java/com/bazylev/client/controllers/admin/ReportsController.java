package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.models.entities.Group;
import com.bazylev.client.models.entities.GroupStudentInfo;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class ReportsController {

    @FXML private ComboBox<String>     reportTypeCombo;
    @FXML private ComboBox<GroupItem>  groupCombo;
    @FXML private DatePicker           fromDate;
    @FXML private DatePicker           toDate;
    @FXML private ComboBox<String>     formatCombo;
    @FXML private TextArea             previewArea;
    @FXML private Button               saveCsvButton;

    private final Gson gson = GsonFactory.getInstance();

    private String lastCsvContent;
    private byte[] lastPdfBytes;
    private boolean isCertificate = false;
    private boolean isReady = false;

    @FXML
    public void initialize() {
        reportTypeCombo.setItems(FXCollections.observableArrayList(
                "ATTENDANCE", "GRADES", "FINANCIAL"));
        reportTypeCombo.getSelectionModel().selectFirst();

        formatCombo.setItems(FXCollections.observableArrayList("CSV", "PDF"));
        formatCombo.getSelectionModel().selectFirst();

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
        }
    }

    @FXML
    private void onTypeChange() {
    }

    @FXML
    private void onGenerate() {
        if (!validateReport()) return;

        JsonObject params = new JsonObject();
        params.addProperty("reportType", reportTypeCombo.getValue());
        params.addProperty("format",     formatCombo.getValue());

        if (groupCombo.getValue() != null) {
            params.addProperty("groupId", groupCombo.getValue().id());
        }
        if (fromDate.getValue() != null) {
            params.addProperty("from", fromDate.getValue().toString());
        }
        if (toDate.getValue() != null) {
            params.addProperty("to", toDate.getValue().toString());
        }

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GENERATE_REPORT, params.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            JsonObject result = JsonParser.parseString(resp.getData()).getAsJsonObject();
            String format   = result.get("format").getAsString();
            String content  = result.get("content").getAsString();
            isCertificate   = false;
            isReady         = true;

            if ("CSV".equals(format)) {
                lastCsvContent = content;
                lastPdfBytes   = null;
                String enriched = enrichCsvWithNames(lastCsvContent, groupCombo.getValue());
                previewArea.setText(enriched);
            } else {
                lastPdfBytes   = Base64.getDecoder().decode(content);
                lastCsvContent = null;
                previewArea.setText("[PDF сформирован — " + lastPdfBytes.length
                        + " байт]\nНажмите «Сохранить PDF» для сохранения файла.");
            }

            updateButtonStates();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private String enrichCsvWithNames(String csv, GroupItem groupItem) {
        if (groupItem == null || csv == null) return csv;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("groupId", groupItem.id());
            Response resp = ServerConnection.getInstance()
                    .send(RequestBuilder.ofRaw(
                            RequestType.GET_STUDENTS_BY_GROUP, data.toString()));
            if (resp.getStatus() != ResponseStatus.OK || resp.getData() == null) return csv;

            List<GroupStudentInfo> students = gson.fromJson(resp.getData(),
                    new TypeToken<List<GroupStudentInfo>>(){}.getType());

            java.util.Map<String, String> gsIdToName = new java.util.HashMap<>();
            for (GroupStudentInfo gs : students) {
                gsIdToName.put(String.valueOf(gs.getId()), gs.getFullName());
            }

            String[] lines = csv.split("\n", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i == 0) {
                    sb.append(line.replaceFirst("ID зачисления", "Студент"));
                } else if (!line.isBlank()) {
                    int comma = line.indexOf(',');
                    if (comma > 0) {
                        String gsId = line.substring(0, comma).strip();
                        String rest = line.substring(comma);
                        String name = gsIdToName.getOrDefault(gsId, "Студент #" + gsId);
                        sb.append(name).append(rest);
                    } else {
                        sb.append(line);
                    }
                } else {
                    sb.append(line);
                }
                if (i < lines.length - 1) sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return csv;
        }
    }

    @FXML
    private void onCertificate() {
        GroupItem group = groupCombo.getValue();
        if (group == null) {
            AlertUtil.showWarning("Выбор", "Выберите группу");
            return;
        }

        JsonObject dataReq = new JsonObject();
        dataReq.addProperty("groupId", group.id());
        Response studResp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(
                        RequestType.GET_STUDENTS_BY_GROUP, dataReq.toString()));

        if (studResp.getStatus() != ResponseStatus.OK || studResp.getData() == null) {
            AlertUtil.showError("Ошибка", "Не удалось загрузить студентов группы");
            return;
        }

        List<GroupStudentInfo> students = gson.fromJson(studResp.getData(),
                new TypeToken<List<GroupStudentInfo>>(){}.getType());

        if (students.isEmpty()) {
            AlertUtil.showWarning("Нет студентов", "В выбранной группе нет студентов");
            return;
        }

        ChoiceDialog<GroupStudentInfo> dialog = new ChoiceDialog<>(students.get(0), students);
        dialog.setTitle("Генерация сертификата");
        dialog.setHeaderText("Выберите студента для сертификата");
        dialog.setContentText("Студент:");

        dialog.showAndWait().ifPresent(selected -> generateCertificate(group.id(), selected));
    }

    private void generateCertificate(int groupId, GroupStudentInfo student) {
        JsonObject params = new JsonObject();
        params.addProperty("studentId", student.getStudentId());
        params.addProperty("groupId",   groupId);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.GENERATE_CERTIFICATE, params.toString()));

        if (resp.getStatus() == ResponseStatus.OK) {
            JsonObject result = JsonParser.parseString(resp.getData()).getAsJsonObject();
            lastPdfBytes   = Base64.getDecoder().decode(result.get("content").getAsString());
            lastCsvContent = null;
            isCertificate  = true;
            isReady        = true;

            previewArea.setText(
                    "Сертификат сформирован для: " + student.getFullName() + "\n" +
                    "Средний балл: " + result.get("average").getAsDouble() + "\n\n" +
                    "Нажмите «Сохранить PDF» для сохранения сертификата.");

            updateButtonStates();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private void updateButtonStates() {
        if (saveCsvButton != null) {
            boolean canSaveCsv = isReady && !isCertificate && lastCsvContent != null;
            saveCsvButton.setVisible(canSaveCsv);
            saveCsvButton.setManaged(canSaveCsv);
        }
    }

    @FXML
    private void onSaveCsv() {
        if (!isReady || isCertificate || lastCsvContent == null) {
            AlertUtil.showWarning("Нет данных", "Сначала сформируйте CSV-отчёт");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
        chooser.setInitialFileName("report.csv");
        File file = chooser.showSaveDialog(previewArea.getScene().getWindow());
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(lastCsvContent);
            AlertUtil.showInfo("Сохранено", "Файл сохранён: " + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось сохранить файл: " + e.getMessage());
        }
    }

    @FXML
    private void onSavePdf() {
        if (!isReady || lastPdfBytes == null) {
            AlertUtil.showWarning("Нет данных", "Сначала сформируйте PDF-отчёт или сертификат");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"));
        chooser.setInitialFileName(isCertificate ? "certificate.pdf" : "report.pdf");
        File file = chooser.showSaveDialog(previewArea.getScene().getWindow());
        if (file == null) return;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(lastPdfBytes);
            AlertUtil.showInfo("Сохранено", "Файл сохранён: " + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось сохранить файл: " + e.getMessage());
        }
    }

    private boolean validateReport() {
        if (reportTypeCombo.getValue() == null) {
            AlertUtil.showWarning("Валидация", "Выберите тип отчёта");
            return false;
        }
        return true;
    }

    record GroupItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
