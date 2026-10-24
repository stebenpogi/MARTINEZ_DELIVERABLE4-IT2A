package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {
    public static final String DB_URL = "jdbc:sqlite:Ebike_list.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Enable SQLite Foreign Key constraints on every connection initialization
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA foreign_keys = ON")) {
            ps.execute();
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }
}