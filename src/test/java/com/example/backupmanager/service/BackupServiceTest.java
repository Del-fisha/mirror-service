package com.example.backupmanager.service;

import com.example.backupmanager.entity.BackupConfig;
import com.example.backupmanager.model.BackupFileRow;
import com.example.backupmanager.repository.BackupConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private OsCommandRunner osCommandRunner;

    @Mock
    private BackupConfigRepository backupConfigRepository;

    private ExecutorService executorService;

    private BackupService backupService;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void prepare() {
        executorService = Executors.newSingleThreadExecutor();
        backupService = new BackupService(osCommandRunner, backupConfigRepository, executorService);
    }

    @AfterEach
    void cleanup() {
        executorService.shutdownNow();
    }

    @Test
    void listsBackupFilesWithBackupExtension() throws Exception {
        Files.createFile(tempDirectory.resolve("a.backup"));
        Files.createFile(tempDirectory.resolve("skip.txt"));
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(configForPath(tempDirectory)));
        List<BackupFileRow> rows = backupService.listBackupFiles();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getFileName()).isEqualTo("a.backup");
    }

    @Test
    void deleteBackupFileRemovesFile() throws Exception {
        Path file = tempDirectory.resolve("drop.backup");
        Files.createFile(file);
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(configForPath(tempDirectory)));
        backupService.deleteBackupFile("drop.backup");
        assertThat(file).doesNotExist();
    }

    @Test
    void deleteBackupFileRejectsPathTraversal() {
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(configForPath(tempDirectory)));
        assertThatThrownBy(() -> backupService.deleteBackupFile("../outside.backup")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBackupNowInvokesPgDump() throws Exception {
        when(osCommandRunner.execute(anyList(), anyMap(), any())).thenReturn(0);
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(configForPath(tempDirectory)));
        backupService.createBackupNow(line -> {
        }).join();
        verify(osCommandRunner).execute(
                argThat(command ->
                        command.get(0).equals("pg_dump")
                                && command.contains("-Fc")
                                && command.contains("-d")
                                && command.contains("appdb")),
                anyMap(),
                any());
    }

    @Test
    void restoreBackupInvokesPgRestoreWithFlags() throws Exception {
        Path file = tempDirectory.resolve("restore.backup");
        Files.createFile(file);
        when(osCommandRunner.execute(anyList(), anyMap(), any())).thenReturn(0);
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(configForPath(tempDirectory)));
        backupService.restoreBackup("restore.backup", line -> {
        }).join();
        verify(osCommandRunner).execute(
                argThat(command ->
                        command.get(0).equals("pg_restore")
                                && command.contains("--clean")
                                && command.contains("--if-exists")),
                anyMap(),
                any());
    }

    private BackupConfig configForPath(Path path) {
        BackupConfig backupConfig = new BackupConfig();
        backupConfig.setId(1L);
        backupConfig.setBackupPath(path.toString());
        backupConfig.setDbHost("localhost");
        backupConfig.setDbPort(5432);
        backupConfig.setDbName("appdb");
        backupConfig.setDbUser("user");
        backupConfig.setDbPassword("pwd");
        backupConfig.setBackupIntervalMinutes(5);
        backupConfig.setBackupEnabled(false);
        return backupConfig;
    }
}
