package com.example.backupmanager.service;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class PostgresConnectionVerifier {

    private final JdbcConnectionFactory jdbcConnectionFactory;

    public PostgresConnectionVerifier(JdbcConnectionFactory jdbcConnectionFactory) {
        this.jdbcConnectionFactory = jdbcConnectionFactory;
    }

    public String verify(String host, int port, String database, String user, String password) {
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        try (Connection connection = jdbcConnectionFactory.open(jdbcUrl, user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            if (resultSet.next() && resultSet.getInt(1) == 1) {
                return "Connection successful";
            }
            return "Unexpected query result";
        } catch (SQLException exception) {
            return "Connection failed: " + exception.getMessage();
        }
    }
}
