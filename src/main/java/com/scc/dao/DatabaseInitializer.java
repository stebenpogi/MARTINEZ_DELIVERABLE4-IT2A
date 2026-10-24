package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseInitializer {

    private static final String USERS = """
        CREATE TABLE IF NOT EXISTS Users (
            Id       INTEGER PRIMARY KEY AUTOINCREMENT,
            Username TEXT NOT NULL UNIQUE,
            Password TEXT NOT NULL,
            Roles    TEXT NOT NULL,
            Status   TEXT NOT NULL DEFAULT 'PENDING'
        )
        """;

    private static final String CUSTOMER = """
        CREATE TABLE IF NOT EXISTS Customer (
            CustomerID     INTEGER PRIMARY KEY AUTOINCREMENT,
            UserID         INTEGER NOT NULL UNIQUE,
            Fullname       TEXT NOT NULL,
            Contact_Number TEXT,
            Address        TEXT,
            Email          TEXT,
            Status         TEXT NOT NULL DEFAULT 'Active',
            FOREIGN KEY (UserID) REFERENCES Users(Id)
        )
        """;

    private static final String EBIKE = """
        CREATE TABLE IF NOT EXISTS Ebike (
            EbikeID INTEGER PRIMARY KEY AUTOINCREMENT,
            Brand   TEXT NOT NULL,
            Model   TEXT NOT NULL,
            Price   REAL NOT NULL,
            Stock   INTEGER NOT NULL DEFAULT 0,
            Status  TEXT NOT NULL DEFAULT 'Available'
        )
        """;

    private static final String LOAN = """
        CREATE TABLE IF NOT EXISTS Loan (
            LoanID        INTEGER PRIMARY KEY AUTOINCREMENT,
            CustomerID    INTEGER NOT NULL,
            EbikeID       INTEGER NOT NULL,
            DownPayment   REAL NOT NULL DEFAULT 0,
            TermMonths    INTEGER NOT NULL,
            MonthlyAmount REAL NOT NULL,
            Loan_Status   TEXT NOT NULL DEFAULT 'Pending',
            FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
            FOREIGN KEY (EbikeID)    REFERENCES Ebike(EbikeID)
        )
        """;

    private static final String PAYMENT = """
        CREATE TABLE IF NOT EXISTS Payment (
            PaymentID     INTEGER PRIMARY KEY AUTOINCREMENT,
            LoanID        INTEGER NOT NULL,
            AmountPaid    REAL NOT NULL,
            PaymentDate   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ReceiptNumber TEXT,
            FOREIGN KEY (LoanID) REFERENCES Loan(LoanID)
        )
        """;

    private static final String ACTIVITY_LOGS = """
        CREATE TABLE IF NOT EXISTS Activity_Logs (
            LogID       INTEGER PRIMARY KEY AUTOINCREMENT,
            UserID      INTEGER NOT NULL,
            Action      TEXT NOT NULL,
            Record_ID   INTEGER,
            Description TEXT,
            Timestamp   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (UserID) REFERENCES Users(Id)
        )
        """;

    private static final String DEFAULT_ADMIN = """
        INSERT OR IGNORE INTO Users (Username, Password, Roles, Status)
        VALUES ('admin', 'admin123', 'ADMIN', 'APPROVED')
        """;

    public static boolean initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String[] tables = {USERS, CUSTOMER, EBIKE, LOAN, PAYMENT, ACTIVITY_LOGS};
            for (String ddl : tables) {
                try (PreparedStatement ps = conn.prepareStatement(ddl)) {
                    ps.execute();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(DEFAULT_ADMIN)) {
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Database setup failed: " + e.getMessage());
            return false;
        }
    }
}