package com.example.backupmanager.config;

import com.example.backupmanager.service.JdbcConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.DriverManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ApplicationExecutorsConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Qualifier("backupOperationExecutor")
    public ExecutorService backupOperationExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "backup-operation");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public JdbcConnectionFactory jdbcConnectionFactory() {
        return DriverManager::getConnection;
    }
}
