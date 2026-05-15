package com.bazylev.client.controllers.admin;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.enums.ResponseStatus;
import com.bazylev.client.enums.Role;
import com.bazylev.client.models.entities.Person;
import com.bazylev.client.models.entities.User;
import com.bazylev.client.models.tcp.Response;
import com.bazylev.client.network.GsonFactory;
import com.bazylev.client.network.ServerConnection;
import com.bazylev.client.util.AlertUtil;
import com.bazylev.client.util.RequestBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label countLabel;

    @FXML private TableView<User>             table;
    @FXML private TableColumn<User, Integer>  colId;
    @FXML private TableColumn<User, String>   colLogin;
    @FXML private TableColumn<User, String>   colFullName;
    @FXML private TableColumn<User, String>   colRole;
    @FXML private TableColumn<User, String>   colBlocked;

    private final Gson gson = GsonFactory.getInstance();
    private final ObservableList<User> all      = FXCollections.observableArrayList();
    private       FilteredList<User>   filtered;
    private final Map<Integer, String> personNameById = new HashMap<>();

    @FXML
    public void initialize() {
        loadPersons();

        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colLogin.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLogin()));
        colFullName.setCellValueFactory(d ->
                new SimpleStringProperty(personNameById.getOrDefault(
                        d.getValue().getPersonId(),
                        "—")));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(formatRole(d.getValue().getRole())));
        colBlocked.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isBlocked() ? "🔒 Заблокирован" : "✅ Активен"));

        roleFilter.setItems(FXCollections.observableArrayList(
                "Все", "ADMIN", "TEACHER", "STUDENT"));
        roleFilter.getSelectionModel().selectFirst();

        filtered = new FilteredList<>(all, u -> true);
        table.setItems(filtered);
        load();
    }

    private void loadPersons() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_PERSONS));
        if (resp.getStatus() == ResponseStatus.OK && resp.getData() != null) {
            List<Person> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<Person>>(){}.getType());
            personNameById.clear();
            for (Person p : list) {
                personNameById.put(p.getId(), safeFullName(p));
            }
        }
    }

    private void load() {
        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.of(RequestType.GET_ALL_USERS));
        if (resp.getStatus() == ResponseStatus.OK) {
            List<User> list = gson.fromJson(resp.getData(),
                    new TypeToken<List<User>>(){}.getType());
            all.setAll(list);
            table.refresh();
            updateCount();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML private void onSearch()  { applyFilters(); }
    @FXML private void onFilter()  { applyFilters(); }

    @FXML private void onReset() {
        searchField.clear();
        roleFilter.getSelectionModel().selectFirst();
        applyFilters();
    }

    private void applyFilters() {
        String q    = searchField.getText().strip().toLowerCase();
        String role = roleFilter.getValue();
        filtered.setPredicate(u -> {
            boolean matchLogin = q.isBlank() || u.getLogin().toLowerCase().contains(q);
            boolean matchRole  = role == null || role.equals("Все")
                    || u.getRole().name().equals(role);
            return matchLogin && matchRole;
        });
        updateCount();
    }

    @FXML
    private void onCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/user-form.fxml"));
            javafx.scene.Parent root = loader.load();
            UserFormController ctrl = loader.getController();
            ctrl.setOnSaved(this::load);

            Stage modal = new Stage();
            modal.setTitle("Создать пользователя");
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEdit() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) {
            AlertUtil.showWarning("Выбор", "Выберите пользователя для редактирования");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/bazylev/client/views/admin/user-edit-form.fxml"));
            javafx.scene.Parent root = loader.load();
            UserEditFormController ctrl = loader.getController();
            ctrl.setUser(u);
            ctrl.setOnSaved(() -> {
                loadPersons();
                load();
            });

            Stage modal = new Stage();
            modal.setTitle("Редактировать пользователя — " + u.getLogin());
            modal.setScene(new javafx.scene.Scene(root));
            modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (IOException e) {
            AlertUtil.showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onBlock() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { AlertUtil.showWarning("Выбор", "Выберите пользователя"); return; }

        boolean block = !u.isBlocked();
        String action = block ? "заблокировать" : "разблокировать";
        if (!AlertUtil.showConfirm("Подтверждение",
                "Вы хотите " + action + " пользователя «" + u.getLogin() + "»?")) return;

        JsonObject data = new JsonObject();
        data.addProperty("userId",  u.getId());
        data.addProperty("blocked", block);

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.BLOCK_USER, data.toString()));
        if (resp.getStatus() == ResponseStatus.OK) {
            load();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) {
            AlertUtil.showWarning("Выбор", "Выберите пользователя");
            return;
        }

        boolean isStudent = u.getRole() != null
                && u.getRole().name().equals("STUDENT");

        String warning = isStudent
                ? "Удалить пользователя «" + u.getLogin() + "»?\n"
                + "Так как это студент, также будет удалена его запись студента."
                : "Удалить пользователя «" + u.getLogin() + "»?";

        if (!AlertUtil.showConfirm("Удаление", warning)) return;

        JsonObject data = new JsonObject();
        data.addProperty("id", u.getId());

        Response resp = ServerConnection.getInstance()
                .send(RequestBuilder.ofRaw(RequestType.DELETE_USER, data.toString()));
        if (resp.getStatus() == ResponseStatus.OK) {
            load();
        } else {
            AlertUtil.showError("Ошибка", resp.getMessage());
        }
    }

    private String formatRole(Role role) {
        if (role == null) return "";
        return switch (role) {
            case ADMIN   -> "Администратор";
            case TEACHER -> "Преподаватель";
            case STUDENT -> "Студент";
        };
    }

    private void updateCount() { countLabel.setText("Всего: " + filtered.size()); }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   == null ? "" : p.getLastName().strip();
        String fn = p.getFirstName()  == null ? "" : p.getFirstName().strip();
        String mn = p.getMiddleName() == null ? "" : p.getMiddleName().strip();
        String fio = (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
        return fio.isBlank() ? ("Person #" + p.getId()) : fio;
    }
}
