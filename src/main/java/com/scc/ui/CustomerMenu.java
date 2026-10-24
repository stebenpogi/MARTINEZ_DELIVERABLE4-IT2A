package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.model.User;
import java.sql.*;
import java.util.Scanner;

public class CustomerMenu {
    private final Scanner scanner;

    public CustomerMenu(Scanner scanner, Object userDao) {
        this.scanner = scanner;
    }

    public void display(User customer) {
        int customerId = getCustomerId(customer.Id());
        if (customerId == -1) {
            System.out.println("\nYour customer profile is not yet registered. Please contact staff.");
            return;
        }

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("            CUSTOMER PORTAL            ");
            System.out.println("==========================================");
            System.out.println("1. Browse Available E-Bikes & Apply for Credit ('Utang')");
            System.out.println("2. View My Loan Schedule & Payment History");
            System.out.println("3. Cancel Pending Application");
            System.out.println("4. Logout");
            System.out.print("Choose option (1-4): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> browseAndApply(customerId);
                case "2" -> viewMyLoans(customerId);
                case "3" -> cancelLoan(customerId);
                case "4" -> { System.out.println("Logged out."); loggedIn = false; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private int getCustomerId(int userId) {
        String sql = "SELECT CustomerID FROM Customer WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("CustomerID");
            }
        } catch (SQLException e) {}
        return -1;
    }

    private void browseAndApply(int customerId) {
        String sql = "SELECT * FROM Ebike WHERE Status = 'Available'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- AVAILABLE E-BIKES ---");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("ID: %d | %s %s | Price: %.2f | Stock: %d\n",
                        rs.getInt("EbikeID"), rs.getString("Brand"),
                        rs.getString("Model"), rs.getDouble("Price"), rs.getInt("Stock"));
            }
            if (!any) { System.out.println("No e-bikes available."); return; }

            System.out.print("\nEnter EbikeID to apply for: ");
            int ebikeId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Down Payment Amount: ");
            double dp = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Term (Months): ");
            int term = Integer.parseInt(scanner.nextLine().trim());

            PreparedStatement ps = conn.prepareStatement("SELECT Price FROM Ebike WHERE EbikeID = ?");
            ps.setInt(1, ebikeId);
            ResultSet prs = ps.executeQuery();
            if (prs.next()) {
                double price = prs.getDouble("Price");
                double monthly = (price - dp) / term;

                String loanSql = "INSERT INTO Loan (CustomerID, EbikeID, DownPayment, TermMonths, MonthlyAmount, Loan_Status) VALUES (?, ?, ?, ?, ?, 'Pending')";
                PreparedStatement lps = conn.prepareStatement(loanSql);
                lps.setInt(1, customerId); lps.setInt(2, ebikeId); lps.setDouble(3, dp);
                lps.setInt(4, term); lps.setDouble(5, monthly);
                lps.executeUpdate();
                System.out.printf("Credit application submitted successfully! Monthly Amortization: %.2f\n", monthly);
            }
        } catch (SQLException | NumberFormatException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void viewMyLoans(int customerId) {
        String sql = "SELECT l.LoanID, e.Brand, e.Model, l.DownPayment, l.TermMonths, l.MonthlyAmount, l.Loan_Status " +
                "FROM Loan l JOIN Ebike e ON l.EbikeID = e.EbikeID WHERE l.CustomerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            System.out.println("\n--- MY LOAN APPLICATIONS & HISTORY ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                int lid = rs.getInt("LoanID");
                System.out.printf("LoanID: %d | E-Bike: %s %s | Downpayment: %.2f | Months: %d | Monthly: %.2f | Status: %s\n",
                        lid, rs.getString("Brand"), rs.getString("Model"),
                        rs.getDouble("DownPayment"), rs.getInt("TermMonths"),
                        rs.getDouble("MonthlyAmount"), rs.getString("Loan_Status"));

                PreparedStatement pps = conn.prepareStatement("SELECT * FROM Payment WHERE LoanID = ?");
                pps.setInt(1, lid);
                ResultSet prs = pps.executeQuery();
                System.out.println("   Payment History:");
                while (prs.next()) {
                    System.out.printf("    - Date: %s | Amount: %.2f | Receipt: %s\n",
                            prs.getString("PaymentDate"), prs.getDouble("AmountPaid"), prs.getString("ReceiptNumber"));
                }
            }
            if (!found) System.out.println("No loans found.");
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void cancelLoan(int customerId) {
        System.out.print("Enter LoanID of pending application to cancel: ");
        try {
            int lid = Integer.parseInt(scanner.nextLine().trim());
            String sql = "DELETE FROM Loan WHERE LoanID = ? AND CustomerID = ? AND Loan_Status = 'Pending'";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, lid); ps.setInt(2, customerId);
                int rows = ps.executeUpdate();
                if (rows > 0) System.out.println("Pending application successfully cancelled.");
                else System.out.println("Cancellation failed (Must be in 'Pending' status).");
            }
        } catch (SQLException | NumberFormatException e) { System.out.println("Invalid input."); }
    }
}