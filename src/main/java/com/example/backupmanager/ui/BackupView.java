package com.example.backupmanager.ui;

import com.example.backupmanager.model.BackupFileRow;
import com.example.backupmanager.service.BackupService;
import com.example.backupmanager.util.ByteSizeFormatter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Backups")
public class BackupView extends VerticalLayout {

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BackupService backupService;

    private final Grid<BackupFileRow> grid;

    private final TextArea operationLog;

    private final Button refreshButton;

    private final Button createBackupButton;

    private final Button restoreButton;

    private final Button deleteButton;

    public BackupView(BackupService backupService) {
        this.backupService = backupService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        grid = new Grid<>(BackupFileRow.class, false);
        grid.addColumn(BackupFileRow::getFileName).setHeader("File name").setFlexGrow(1);
        grid.addColumn(row -> ByteSizeFormatter.format(row.getSizeBytes())).setHeader("Size").setWidth("10em").setFlexGrow(0);
        grid.addColumn(row -> DISPLAY_TIME.format(row.getLastModified())).setHeader("Last modified").setWidth("14em").setFlexGrow(0);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(event -> updateActionButtonsState());
        refreshButton = new Button("Refresh", event -> refreshGrid());
        createBackupButton = new Button("Create backup now", event -> startManualBackup());
        restoreButton = new Button("Restore", event -> confirmAndRestore());
        deleteButton = new Button("Delete", event -> deleteSelected());
        restoreButton.setEnabled(false);
        deleteButton.setEnabled(false);
        HorizontalLayout toolbar = new HorizontalLayout(refreshButton, createBackupButton, restoreButton, deleteButton);
        toolbar.setWidthFull();
        operationLog = new TextArea("Operation output");
        operationLog.setWidthFull();
        operationLog.setReadOnly(true);
        operationLog.setMinHeight("240px");
        operationLog.setSizeFull();
        add(toolbar, grid, operationLog);
        setFlexGrow(1, grid);
        setFlexGrow(0, toolbar);
        setFlexGrow(1, operationLog);
        expand(grid);
        expand(operationLog);
        refreshGrid();
    }

    private void updateActionButtonsState() {
        boolean selected = grid.asSingleSelect().getValue() != null;
        restoreButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
    }

    private void refreshGrid() {
        grid.setItems(backupService.listBackupFiles());
        updateActionButtonsState();
    }

    private void startManualBackup() {
        appendOutput("Starting manual backup");
        backupService
                .createBackupNow(line -> getUI().ifPresent(ui -> ui.access(() -> appendOutput(line))))
                .whenComplete((result, error) -> getUI().ifPresent(ui -> ui.access(() -> {
                    if (error != null) {
                        appendOutput("Manual backup failed: " + rootMessage(error));
                        Notification.show("Backup failed", 4000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        appendOutput("Manual backup finished");
                        Notification.show("Backup completed", 3000, Notification.Position.MIDDLE);
                    }
                    refreshGrid();
                })));
    }

    private void confirmAndRestore() {
        BackupFileRow selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            return;
        }
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirm restore");
        confirmDialog.setText("Restore database from \"" + selected.getFileName() + "\"? Existing database objects may be removed.");
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Restore");
        confirmDialog.setCancelText("Cancel");
        confirmDialog.addConfirmListener(confirmEvent -> runRestore(selected.getFileName()));
        confirmDialog.open();
    }

    private void runRestore(String fileName) {
        appendOutput("Starting restore for " + fileName);
        backupService
                .restoreBackup(fileName, line -> getUI().ifPresent(ui -> ui.access(() -> appendOutput(line))))
                .whenComplete((result, error) -> getUI().ifPresent(ui -> ui.access(() -> {
                    if (error != null) {
                        appendOutput("Restore failed: " + rootMessage(error));
                        Notification.show("Restore failed", 5000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        appendOutput("Restore finished");
                        Notification.show("Restore completed", 4000, Notification.Position.MIDDLE);
                    }
                    refreshGrid();
                })));
    }

    private void deleteSelected() {
        BackupFileRow selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            return;
        }
        try {
            backupService.deleteBackupFile(selected.getFileName());
            appendOutput("Deleted file " + selected.getFileName());
            Notification.show("File deleted", 3000, Notification.Position.MIDDLE);
            refreshGrid();
        } catch (RuntimeException exception) {
            Notification.show("Delete failed: " + rootMessage(exception), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void appendOutput(String line) {
        String existing = operationLog.getValue();
        String prefix = existing == null || existing.isEmpty() ? "" : existing + System.lineSeparator();
        operationLog.setValue(prefix + line);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
