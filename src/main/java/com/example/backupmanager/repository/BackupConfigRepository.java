package com.example.backupmanager.repository;

import com.example.backupmanager.entity.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupConfigRepository extends JpaRepository<BackupConfig, Long> {
}
