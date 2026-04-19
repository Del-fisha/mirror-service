package com.example.backupmanager.service;

import com.example.backupmanager.entity.BackupConfig;
import com.example.backupmanager.model.BackupFileRow;
import com.example.backupmanager.repository.BackupConfigRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final DateTimeFormatter BACKUP_FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    private final OsCommandRunner osCommandRunner;

    private final BackupConfigRepository backupConfigRepository;

    private final ExecutorService backupOperationExecutor;

    public BackupService(
            OsCommandRunner osCommandRunner,
            BackupConfigRepository backupConfigRepository,
            @Qualifier("backupOperationExecutor") ExecutorService backupOperationExecutor) {
        this.osCommandRunner = osCommandRunner;
        this.backupConfigRepository = backupConfigRepository;
        this.backupOperationExecutor = backupOperationExecutor;
    }

    public List<BackupFileRow> listBackupFiles() {
        BackupConfig config = readConfig();
        Path directory = Paths.get(config.getBackupPath()).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".backup"))
                    .map(path -> {
                        try {
                            return new BackupFileRow(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    Files.getLastModifiedTime(path).toInstant());
                        } catch (IOException exception) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(BackupFileRow::getLastModified).reversed())
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException exception) {
            return List.of();
        }
    }

    public void deleteBackupFile(String fileName) {
        BackupConfig config = readConfig();
        Path file = resolveBackupFile(config, fileName);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public CompletableFuture<Void> createBackupNow(Consumer<String> log) {
        return CompletableFuture.runAsync(() -> executePgDump(readConfig(), log), backupOperationExecutor);
    }

    public CompletableFuture<Void> restoreBackup(String fileName, Consumer<String> log) {
        return CompletableFuture.runAsync(() -> executePgRestore(readConfig(), fileName, log), backupOperationExecutor);
    }

    private void executePgDump(BackupConfig config, Consumer<String> log) {
        try {
            Path directory = Paths.get(config.getBackupPath()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String stamp = BACKUP_FILE_TIMESTAMP.format(ZonedDateTime.now());
            Path outputFile = directory.resolve("backup_" + stamp + ".backup").normalize();
            if (!outputFile.startsWith(directory)) {
                throw new IllegalStateException("Resolved path is outside backup directory");
            }
            List<String> command = List.of(
                    "pg_dump",
                    "-h",
                    config.getDbHost(),
                    "-p",
                    String.valueOf(config.getDbPort()),
                    "-U",
                    config.getDbUser(),
                    "-d",
                    config.getDbName(),
                    "-Fc",
                    "-f",
                    outputFile.toString());
            Map<String, String> environment = new HashMap<>();
            environment.put("PGPASSWORD", defaultString(config.getDbPassword()));
            log.accept("Running pg_dump for file " + outputFile.getFileName());
            int exitCode = osCommandRunner.execute(command, environment, log::accept);
            if (exitCode != 0) {
                throw new IllegalStateException("pg_dump exited with code " + exitCode);
            }
            log.accept("pg_dump completed successfully");
        } catch (Exception exception) {
            log.accept("pg_dump failed: " + exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    private void executePgRestore(BackupConfig config, String fileName, Consumer<String> log) {
        try {
            Path file = resolveBackupFile(config, fileName);
            List<String> command = List.of(
                    "pg_restore",
                    "-h",
                    config.getDbHost(),
                    "-p",
                    String.valueOf(config.getDbPort()),
                    "-U",
                    config.getDbUser(),
                    "-d",
                    config.getDbName(),
                    "--clean",
                    "--if-exists",
                    file.toString());
            Map<String, String> environment = new HashMap<>();
            environment.put("PGPASSWORD", defaultString(config.getDbPassword()));
            log.accept("Running pg_restore for file " + fileName);
            int exitCode = osCommandRunner.execute(command, environment, log::accept);
            if (exitCode != 0) {
                throw new IllegalStateException("pg_restore exited with code " + exitCode);
            }
            log.accept("pg_restore completed successfully");
        } catch (Exception exception) {
            log.accept("pg_restore failed: " + exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    private BackupConfig readConfig() {
        return backupConfigRepository.findById(1L).orElseThrow(() -> new IllegalStateException("Backup configuration is missing"));
    }

    private static Path resolveBackupFile(BackupConfig config, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        Path root = Paths.get(config.getBackupPath()).toAbsolutePath().normalize();
        Path resolved = root.resolve(fileName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return resolved;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }
}
