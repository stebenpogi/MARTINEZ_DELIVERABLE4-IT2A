package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.model.User;
import java.sql.*;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner scanner;
    private final UserDao userDao;

    public AdminMenu(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void display(User admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("            ADMIN DASHBOARD            ");
            System.out.println("==========================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. Manage E-Bikes (Add, Update, Remove)");
            System.out.println("8. Review & Approve Loan Applications");
            System.out.println("9. Logout");
            System.out.print("Choose an option (1-9): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printUserTable("ALL USERS", userDao.getAllUsers());
                case "2" -> printUserTable("PENDING USERS", userDao.getPendingUsers());
                case "3" -> approveUser();
                case "4" -> addUser();
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
                case "7" -> manageEbikes(admin.Id());
                case "8" -> reviewPendingLoans(admin.Id());
                case "9" -> { System.out.println("Logged out."); loggedIn = false; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) { System.out.println("No pending users."); return; }
        printUserTable("PENDING USERS", pending);
        System.out.print("Enter User ID to approve: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        System.out.println(userDao.approveUser(id) ? "User approved successfully." : "Approval failed.");
    }

    private void addUser() {
        System.out.print("Username: "); String u = scanner.nextLine().trim();
        System.out.print("Password: "); String p = scanner.nextLine().trim();
        System.out.print("Role (ADMIN/STAFF/CUSTOMER): "); String r = scanner.nextLine().trim();
        System.out.println(userDao.addUser(u, p, r) ? "User added successfully." : "Failed.");
    }

    private void manageEbikes(int adminUserId) {
        boolean managing = true;
        while (managing) {
            System.out.println("\n--- E-BIKE MANAGEMENT ---");
            System.out.println("[1] View All E-Bikes");
            System.out.println("[2] Add New E-Bike");
            System.out.println("[3] Update E-Bike Price & Stock");
            System.out.println("[4] Remove E-Bike");
            System.out.println("[5] Back to Admin Dashboard");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewEbikes();
                case "2" -> addEbike(adminUserId);
                case "3" -> updateEbike(adminUserId);
                case "4" -> deleteEbike(adminUserId);
                case "5" -> managing = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void reviewPendingLoans(int adminUserId) {
        String sql = "SELECT l.LoanID, c.Fullname, e.Brand, e.Model, l.DownPayment, l.TermMonths, l.MonthlyAmount " +
                "FROM Loan l JOIN Customer c ON l.CustomerID = c.CustomerID JOIN Ebike e ON l.EbikeID = e.EbikeID " +
                "WHERE l.Loan_Status = 'Pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- PENDING CREDIT APPLICATIONS ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("LoanID: %d | Customer: %s | E-Bike: %s %s | Downpayment: %.2f | Term: %d mos | Monthly: %.2f\n",
                        rs.getInt("LoanID"), rs.getString("Fullname"), rs.getString("Brand"),
                        rs.getString("Model"), rs.getDouble("DownPayment"), rs.getInt("TermMonths"), rs.getDouble("MonthlyAmount"));
            }
            if (!found) {
                System.out.println("No pending loan applications.");
                return;
            }

            System.out.print("\nEnter LoanID to process (0 to cancel): ");
            int loanId = Integer.parseInt(scanner.nextLine().trim());
            if (loanId == 0) return;

            System.out.print("Action [1] Approve [2] Reject: ");
            String action = scanner.nextLine().trim();
            String newStatus = action.equals("1") ? "Approved" : "Rejected";

            String updateSql = "UPDATE Loan SET Loan_Status = ? WHERE LoanID = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, loanId);
                ps.executeUpdate();
                System.out.println("Loan application status updated to: " + newStatus);
                logAction(adminUserId, "REVIEW_LOAN", loanId, "Set loan status to " + newStatus);
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void viewEbikes() {
        String sql = "SELECT * FROM Ebike";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nID | Brand | Model | Price | Stock | Status");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %.2f | %d | %s\n",
                        rs.getInt("EbikeID"), rs.getString("Brand"), rs.getString("Model"),
                        rs.getDouble("Price"), rs.getInt("Stock"), rs.getString("Status"));
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void addEbike(int adminUserId) {
        System.out.print("Brand: "); String brand = scanner.nextLine().trim();
        System.out.print("Model: "); String model = scanner.nextLine().trim();
        System.out.print("Price: "); double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Stock: "); int stock = Integer.parseInt(scanner.nextLine().trim());

        String sql = "INSERT INTO Ebike (Brand, Model, Price, Stock, Status) VALUES (?, ?, ?, ?, 'Available')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brand); ps.setString(2, model); ps.setDouble(3, price); ps.setInt(4, stock);
            ps.executeUpdate();
            System.out.println("E-Bike added successfully!");
            logAction(adminUserId, "ADD_EBIKE", 0, "Added e-bike: " + brand + " " + model);
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void updateEbike(int adminUserId) {
        viewEbikes();
        System.out.print("Enter EbikeID to update: "); int id = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("New Price: "); double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("New Stock: "); int stock = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Status (Available/Unavailable): "); String status = scanner.nextLine().trim();

        String sql = "UPDATE Ebike SET Price = ?, Stock = ?, Status = ? WHERE EbikeID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, price); ps.setInt(2, stock); ps.setString(3, status); ps.setInt(4, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("E-Bike updated successfully!");
                logAction(adminUserId, "UPDATE_EBIKE", id, "Updated e-bike ID " + id);
            } else { System.out.println("EbikeID not found."); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void deleteEbike(int adminUserId) {
        viewEbikes();
        System.out.print("Enter EbikeID to remove: "); int id = Integer.parseInt(scanner.nextLine().trim());
        String sql = "DELETE FROM Ebike WHERE EbikeID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("E-Bike removed successfully!");
                logAction(adminUserId, "DELETE_EBIKE", id, "Removed e-bike ID " + id);
            } else { System.out.println("EbikeID not found."); }
        } catch (SQLException e) { System.err.println("Error (may be linked to active loans): " + e.getMessage()); }
    }

    private void updateUser(User admin) {
        printUserTable("ALL USERS", userDao.getAllUsers());
        System.out.print("Enter User ID to update: "); int id = Integer.parseInt(scanner.nextLine().trim());
        if (id == admin.Id()) { System.out.println("Cannot modify your own account."); return; }
        System.out.print("New Role: "); String r = scanner.nextLine().trim();
        System.out.print("New Status (APPROVED/INACTIVE/PENDING): "); String s = scanner.nextLine().trim();
        System.out.println(userDao.updateRoleAndStatus(id, r, s) ? "User updated." : "Update failed.");
    }

    private void deleteUser(User admin) {
        printUserTable("ALL USERS", userDao.getAllUsers());
        System.out.print("Enter User ID to delete: "); int id = Integer.parseInt(scanner.nextLine().trim());
        if (id == admin.Id()) { System.out.println("Cannot delete your own account."); return; }
        System.out.println(userDao.deleteUser(id) ? "User deleted." : "Delete failed.");
    }

    public static void logAction(int userId, String action, int recordId, String description) {
        String sql = "INSERT INTO Activity_Logs (UserID, Action, Record_ID, Description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, action); ps.setInt(3, recordId); ps.setString(4, description);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Logging error: " + e.getMessage()); }
    }

    private void printUserTable(String title, List<User> users) {
        System.out.println("\n--- " + title + " ---");
        System.out.println("ID | Username | Role | Status");
        for (User u : users)
            System.out.printf("%d | %s | %s | %s\n", u.Id(), u.Username(), u.Roles(), u.Status());
    }
}