package com.example.backupmanager.model;

import java.time.Instant;

public class BackupFileRow {

    private final String fileName;

    private final long sizeBytes;

    private final Instant lastModified;

    public BackupFileRow(String fileName, long sizeBytes, Instant lastModified) {
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getLastModified() {
        return lastModified;
    }
}
