package com.example.backupmanager.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface OsCommandRunner {

    int execute(List<String> command, Map<String, String> environment, Consumer<String> outputLineConsumer) throws Exception;
}
