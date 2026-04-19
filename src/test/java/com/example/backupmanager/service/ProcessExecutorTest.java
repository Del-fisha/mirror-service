package com.example.backupmanager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessExecutorTest {

    private ProcessExecutor processExecutor;

    @AfterEach
    void tearDown() {
        if (processExecutor != null) {
            processExecutor.shutdownStreamReaders();
        }
    }

    @Test
    void capturesJavaVersionOutput() throws Exception {
        processExecutor = new ProcessExecutor();
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        int exitCode = processExecutor.execute(List.of("java", "-version"), Map.of(), lines::add);
        assertThat(exitCode).isZero();
        assertThat(lines).isNotEmpty();
    }
}
