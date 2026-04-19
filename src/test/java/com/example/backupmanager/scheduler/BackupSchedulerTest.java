package com.example.backupmanager.scheduler;

import com.example.backupmanager.entity.BackupConfig;
import com.example.backupmanager.repository.BackupConfigRepository;
import com.example.backupmanager.service.BackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock
    private BackupConfigRepository backupConfigRepository;

    @Mock
    private BackupService backupService;

    @Test
    void rescheduleSkipsWhenAutomaticBackupsDisabled() {
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(disabledConfiguration()));
        when(backupService.createBackupNow(any())).thenReturn(CompletableFuture.completedFuture(null));
        BackupScheduler backupScheduler = new BackupScheduler(backupConfigRepository, backupService);
        backupScheduler.reschedule();
        verify(backupService, never()).createBackupNow(any());
        backupScheduler.shutdown();
    }

    @Test
    void rescheduleTriggersBackupWhenEnabled() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        when(backupConfigRepository.findById(1L)).thenReturn(Optional.of(enabledConfiguration()));
        when(backupService.createBackupNow(any())).thenAnswer(invocation -> {
            latch.countDown();
            java.util.function.Consumer<String> consumer = invocation.getArgument(0);
            consumer.accept("scheduled");
            return CompletableFuture.completedFuture(null);
        });
        BackupScheduler backupScheduler = new BackupScheduler(backupConfigRepository, backupService);
        backupScheduler.reschedule();
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        verify(backupService, timeout(15000).atLeastOnce()).createBackupNow(any());
        backupScheduler.shutdown();
    }

    private static BackupConfig disabledConfiguration() {
        BackupConfig backupConfig = new BackupConfig();
        backupConfig.setId(1L);
        backupConfig.setBackupPath("./backups");
        backupConfig.setDbHost("localhost");
        backupConfig.setDbPort(5432);
        backupConfig.setDbName("postgres");
        backupConfig.setDbUser("postgres");
        backupConfig.setDbPassword("");
        backupConfig.setBackupIntervalMinutes(5);
        backupConfig.setBackupEnabled(false);
        return backupConfig;
    }

    private static BackupConfig enabledConfiguration() {
        BackupConfig backupConfig = disabledConfiguration();
        backupConfig.setBackupEnabled(true);
        backupConfig.setBackupIntervalMinutes(1);
        return backupConfig;
    }
}
