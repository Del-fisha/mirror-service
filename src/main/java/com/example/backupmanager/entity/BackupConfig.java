package com.example.backupmanager.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "backup_config")
public class BackupConfig {

    @Id
    private Long id = 1L;

    private String backupPath;

    private String dbHost;

    private Integer dbPort;

    private String dbName;

    private String dbUser;

    private String dbPassword;

    private Integer backupIntervalMinutes;

    private Boolean backupEnabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public Integer getDbPort() {
        return dbPort;
    }

    public void setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public Integer getBackupIntervalMinutes() {
        return backupIntervalMinutes;
    }

    public void setBackupIntervalMinutes(Integer backupIntervalMinutes) {
        this.backupIntervalMinutes = backupIntervalMinutes;
    }

    public Boolean getBackupEnabled() {
        return backupEnabled;
    }

    public void setBackupEnabled(Boolean backupEnabled) {
        this.backupEnabled = backupEnabled;
    }
}
