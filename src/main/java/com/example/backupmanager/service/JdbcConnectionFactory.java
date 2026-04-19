package com.example.backupmanager.service;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface JdbcConnectionFactory {

    Connection open(String jdbcUrl, String user, String password) throws SQLException;
}
