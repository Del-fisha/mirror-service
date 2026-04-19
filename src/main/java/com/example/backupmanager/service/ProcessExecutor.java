package com.example.backupmanager.service;

import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Component
public class ProcessExecutor implements OsCommandRunner {

    private final ExecutorService streamReaderPool = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "process-exec-stream");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    public void shutdownStreamReaders() {
        streamReaderPool.shutdownNow();
    }

    @Override
    public int execute(List<String> command, Map<String, String> environment, Consumer<String> outputLineConsumer)
            throws Exception {
        List<String> safeCommand = new ArrayList<>(command);
        ProcessBuilder builder = new ProcessBuilder(safeCommand);
        Map<String, String> env = builder.environment();
        env.putAll(environment);
        Process process = builder.start();
        Future<?> outFuture = streamReaderPool.submit(() -> drainStream(process.getInputStream(), outputLineConsumer, false));
        Future<?> errFuture = streamReaderPool.submit(() -> drainStream(process.getErrorStream(), outputLineConsumer, true));
        int exit = process.waitFor();
        outFuture.get();
        errFuture.get();
        return exit;
    }

    private static void drainStream(InputStream stream, Consumer<String> consumer, boolean error) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (error) {
                    consumer.accept("[stderr] " + line);
                } else {
                    consumer.accept(line);
                }
            }
        } catch (Exception exception) {
        }
    }
}
