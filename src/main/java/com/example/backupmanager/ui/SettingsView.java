package com.example.backupmanager.ui;

import com.example.backupmanager.entity.BackupConfig;
import com.example.backupmanager.repository.BackupConfigRepository;
import com.example.backupmanager.scheduler.BackupScheduler;
import com.example.backupmanager.service.PostgresConnectionVerifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
public class SettingsView extends VerticalLayout {

    private final BackupConfigRepository backupConfigRepository;

    private final BackupScheduler backupScheduler;

    private final PostgresConnectionVerifier postgresConnectionVerifier;

    private final Binder<BackupConfig> binder;

    private final TextField backupPathField;

    private final TextField dbHostField;

    private final IntegerField dbPortField;

    private final TextField dbNameField;

    private final TextField dbUserField;

    private final PasswordField dbPasswordField;

    private final IntegerField backupIntervalField;

    private final Checkbox backupEnabledField;

    public SettingsView(
            BackupConfigRepository backupConfigRepository,
            BackupScheduler backupScheduler,
            PostgresConnectionVerifier postgresConnectionVerifier) {
        this.backupConfigRepository = backupConfigRepository;
        this.backupScheduler = backupScheduler;
        this.postgresConnectionVerifier = postgresConnectionVerifier;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        binder = new Binder<>(BackupConfig.class);
        backupPathField = new TextField("Backup folder path");
        backupPathField.setWidthFull();
        dbHostField = new TextField("Database host");
        dbHostField.setWidthFull();
        dbPortField = new IntegerField("Database port");
        dbPortField.setWidthFull();
        dbNameField = new TextField("Database name");
        dbNameField.setWidthFull();
        dbUserField = new TextField("Database user");
        dbUserField.setWidthFull();
        dbPasswordField = new PasswordField("Database password");
        dbPasswordField.setWidthFull();
        backupIntervalField = new IntegerField("Backup interval (minutes)");
        backupIntervalField.setWidthFull();
        backupEnabledField = new Checkbox("Automatic backups enabled");
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.add(
                backupPathField,
                dbHostField,
                dbPortField,
                dbNameField,
                dbUserField,
                dbPasswordField,
                backupIntervalField,
                backupEnabledField);
        binder.forField(backupPathField).asRequired().bind(BackupConfig::getBackupPath, BackupConfig::setBackupPath);
        binder.forField(dbHostField).asRequired().bind(BackupConfig::getDbHost, BackupConfig::setDbHost);
        binder.forField(dbPortField).asRequired().bind(BackupConfig::getDbPort, BackupConfig::setDbPort);
        binder.forField(dbNameField).asRequired().bind(BackupConfig::getDbName, BackupConfig::setDbName);
        binder.forField(dbUserField).asRequired().bind(BackupConfig::getDbUser, BackupConfig::setDbUser);
        binder.forField(dbPasswordField).bind(BackupConfig::getDbPassword, BackupConfig::setDbPassword);
        binder.forField(backupIntervalField).asRequired().bind(BackupConfig::getBackupIntervalMinutes, BackupConfig::setBackupIntervalMinutes);
        binder.forField(backupEnabledField).bind(BackupConfig::getBackupEnabled, BackupConfig::setBackupEnabled);
        Button saveButton = new Button("Save", event -> saveSettings());
        Button testConnectionButton = new Button("Test connection", event -> testConnection());
        add(formLayout, saveButton, testConnectionButton);
        loadSettings();
    }

    private void loadSettings() {
        backupConfigRepository
                .findById(1L)
                .ifPresentOrElse(binder::readBean, () -> Notification.show("Configuration row is missing", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR));
    }

    private void saveSettings() {
        BackupConfig configuration = backupConfigRepository
                .findById(1L)
                .orElseGet(() -> {
                    BackupConfig created = new BackupConfig();
                    created.setId(1L);
                    return created;
                });
        try {
            binder.writeBean(configuration);
        } catch (ValidationException exception) {
            Notification.show("Fix validation errors before saving", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        backupConfigRepository.save(configuration);
        backupScheduler.reschedule();
        Notification.show("Settings saved", 3000, Notification.Position.MIDDLE);
    }

    private void testConnection() {
        if (!binder.validate().isOk()) {
            Notification.show("Fix validation errors before testing", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        String host = dbHostField.getValue();
        Integer port = dbPortField.getValue();
        String database = dbNameField.getValue();
        String user = dbUserField.getValue();
        String password = dbPasswordField.getValue() == null ? "" : dbPasswordField.getValue();
        if (host == null || port == null || database == null || user == null) {
            Notification.show("Host, port, database, and user are required", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        String outcome = postgresConnectionVerifier.verify(host, port, database, user, password);
        Notification notification = Notification.show(outcome, 6000, Notification.Position.MIDDLE);
        if (outcome.startsWith("Connection successful")) {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
