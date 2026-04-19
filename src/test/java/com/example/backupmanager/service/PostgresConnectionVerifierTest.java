package com.example.backupmanager.service;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresConnectionVerifierTest {

    @Test
    void returnsSuccessWhenSelectOneWorks() throws Exception {
        String memoryName = "pgconn" + UUID.randomUUID().toString().replace("-", "");
        JdbcConnectionFactory factory = (url, user, password) ->
                DriverManager.getConnection("jdbc:h2:mem:" + memoryName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        PostgresConnectionVerifier verifier = new PostgresConnectionVerifier(factory);
        String outcome = verifier.verify("db.example.com", 5432, "app", "user", "secret");
        assertThat(outcome).isEqualTo("Connection successful");
    }

    @Test
    void returnsFailureMessageWhenQueryThrows() {
        JdbcConnectionFactory factory = (url, user, password) -> {
            throw new java.sql.SQLException("simulated failure");
        };
        PostgresConnectionVerifier verifier = new PostgresConnectionVerifier(factory);
        String outcome = verifier.verify("localhost", 5432, "postgres", "u", "p");
        assertThat(outcome).startsWith("Connection failed:");
    }
}
