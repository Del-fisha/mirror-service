package com.example.backupmanager.scheduler;

import com.example.backupmanager.entity.BackupConfig;
import com.example.backupmanager.repository.BackupConfigRepository;
import com.example.backupmanager.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Component
public class BackupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupScheduler.class);

    private final ThreadPoolTaskScheduler taskScheduler;

    private final BackupConfigRepository backupConfigRepository;

    private final BackupService backupService;

    private volatile ScheduledFuture<?> scheduledFuture;

    public BackupScheduler(BackupConfigRepository backupConfigRepository, BackupService backupService) {
        this.backupConfigRepository = backupConfigRepository;
        this.backupService = backupService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setThreadNamePrefix("pg-backup-");
        this.taskScheduler.initialize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reschedule();
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        Optional<BackupConfig> optionalConfig = backupConfigRepository.findById(1L);
        if (optionalConfig.isEmpty()) {
            return;
        }
        BackupConfig config = optionalConfig.get();
        if (!Boolean.TRUE.equals(config.getBackupEnabled())) {
            return;
        }
        int minutes = Optional.ofNullable(config.getBackupIntervalMinutes()).orElse(5);
        long periodMilliseconds = Math.max(1, minutes) * 60_000L;
        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::runScheduledBackup, new Date(), periodMilliseconds);
    }

    private void runScheduledBackup() {
        try {
            backupService.createBackupNow(LOGGER::info).join();
        } catch (Exception exception) {
            LOGGER.warn("Scheduled backup failed", exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        this.taskScheduler.shutdown();
    }
}
