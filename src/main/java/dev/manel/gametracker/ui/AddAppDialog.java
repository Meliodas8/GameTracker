package dev.manel.gametracker.ui;

import dev.manel.gametracker.core.ProcessUtils;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.config.I18n;
import dev.manel.gametracker.core.model.DetectedGame;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/** Diálogo para dar de alta una aplicación manual. Compartido por la lista de juegos y ajustes. */
final class AddAppDialog {

    private AddAppDialog() {}

    static Optional<DetectedGame> show() {
        Dialog<DetectedGame> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("dialog.addApp.title"));
        dialog.setHeaderText(null);

        // Aplicar el tema actual al diálogo
        String css = ConfigManager.getInstance().getTheme().equals("dark") ? "styles-dark.css" : "styles.css";
        dialog.getDialogPane().getStylesheets().add(
                AddAppDialog.class.getResource("/dev/manel/gametracker/" + css).toExternalForm()
        );

        ButtonType addButtonType = new ButtonType(I18n.get("common.add"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Campos del formulario
        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.prompt.name"));

        TextField execField = new TextField();
        execField.setPromptText(I18n.get("dialog.prompt.exec"));

        // Lista de procesos en ejecución — misma lógica que ProcessWatcher
        List<ProcessUtils.ProcessInfo> processes = ProcessUtils.getRunningProcessInfosSorted();

        TextField filterField = new TextField();
        filterField.setPromptText(I18n.get("dialog.prompt.filter"));

        ListView<ProcessUtils.ProcessInfo> processList = new ListView<>();
        ObservableList<ProcessUtils.ProcessInfo> allProcesses = FXCollections.observableArrayList(processes);
        processList.setItems(allProcesses);
        processList.setPrefHeight(160);
        processList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProcessUtils.ProcessInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayLabel());
            }
        });

        filterField.textProperty().addListener((obs, old, val) ->
                processList.setItems(val.isBlank()
                        ? allProcesses
                        : allProcesses.filtered(p -> p.displayLabel().toLowerCase().contains(val.toLowerCase())))
        );

        // Al seleccionar un proceso se rellena automáticamente el campo ejecutable
        processList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> { if (selected != null) execField.setText(selected.suggestedExecName()); }
        );

        Label nameLabel = new Label(I18n.get("dialog.field.name"));
        nameLabel.getStyleClass().add("settings-label");
        Label execLabel = new Label(I18n.get("dialog.field.exec"));
        execLabel.getStyleClass().add("settings-label");
        Label processLabel = new Label(I18n.get("dialog.processes.hint"));
        processLabel.getStyleClass().add("settings-description");

        VBox content = new VBox(8,
                nameLabel, nameField,
                execLabel, execField,
                processLabel, filterField, processList
        );
        content.setPadding(new Insets(16));
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);

        // El botón Añadir se activa solo cuando ambos campos tienen texto
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        ChangeListener<String> validator = (obs, old, val) ->
                addButton.setDisable(nameField.getText().isBlank() || execField.getText().isBlank());
        nameField.textProperty().addListener(validator);
        execField.textProperty().addListener(validator);

        dialog.setResultConverter(bt -> bt == addButtonType
                ? new DetectedGame(nameField.getText().trim(), "MANUAL", execField.getText().trim(), null)
                : null
        );

        return dialog.showAndWait();
    }
}
