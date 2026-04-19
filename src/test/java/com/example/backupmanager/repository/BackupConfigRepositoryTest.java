package com.example.backupmanager.repository;

import com.example.backupmanager.entity.BackupConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BackupConfigRepositoryTest {

    @Autowired
    private BackupConfigRepository backupConfigRepository;

    @Test
    void loadsSeededConfiguration() {
        assertThat(backupConfigRepository.findById(1L)).isPresent();
    }

    @Test
    void updatesExistingConfiguration() {
        BackupConfig configuration = backupConfigRepository.findById(1L).orElseThrow();
        configuration.setDbHost("updated.example");
        backupConfigRepository.save(configuration);
        assertThat(backupConfigRepository.findById(1L).orElseThrow().getDbHost()).isEqualTo("updated.example");
    }
}
