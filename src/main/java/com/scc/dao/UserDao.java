package com.scc.dao;

import com.scc.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("Id"), rs.getString("Username"),
                rs.getString("Password"), rs.getString("Roles"), rs.getString("Status"));
    }

    public boolean registerUser(String username, String password, String roles) {
        String sql = "INSERT INTO Users (Username, Password, Roles, Status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, roles.toUpperCase());
            pstmt.execute();
            System.out.println("-> Successful Registration, Waiting For Approval!");
            return true;
        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean addUser(String username, String password, String roles) {
        String sql = "INSERT INTO Users (Username, Password, Roles, Status) VALUES (?, ?, ?, 'APPROVED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, roles.toUpperCase());
            pstmt.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
            return false;
        }
    }

    public User findForLogin(String username, String password, String roles) {
        String sql = "SELECT * FROM Users WHERE Username = ? AND Password = ? AND Roles = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, roles.toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() { return query("SELECT * FROM Users ORDER BY Id"); }
    public List<User> getPendingUsers() { return query("SELECT * FROM Users WHERE Status = 'PENDING' ORDER BY Id"); }

    private List<User> query(String sql) {
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
        }
        return list;
    }

    public boolean approveUser(int userId) {
        String sql = "UPDATE Users SET Status = 'APPROVED' WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean updateRoleAndStatus(int id, String role, String status) {
        String sql = "UPDATE Users SET Roles = ?, Status = ? WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.toUpperCase());
            pstmt.setString(2, status.toUpperCase());
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM Users WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}