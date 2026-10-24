package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.model.User;
import java.sql.*;
import java.util.Scanner;

public class StaffMenu {
    private final Scanner scanner;

    public StaffMenu(Scanner scanner, Object userDao) {
        this.scanner = scanner;
    }

    public void display(User staff) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("             STAFF DASHBOARD             ");
            System.out.println("==========================================");
            System.out.println("1. Register Customer Profile");
            System.out.println("2. Encode Credit Application ('Utang')");
            System.out.println("3. Record Payment & Issue Receipt");
            System.out.println("4. View Customer Balances");
            System.out.println("5. View Available E-Bikes");
            System.out.println("6. Logout");
            System.out.print("Choose option (1-6): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registerCustomer(staff.Id());
                case "2" -> encodeLoan(staff.Id());
                case "3" -> recordPayment(staff.Id());
                case "4" -> viewBalances();
                case "5" -> viewAvailableEbikes();
                case "6" -> { System.out.println("Logged out."); loggedIn = false; }
                default -> System.out.println("Invalid option. Choose 1-6.");
            }
        }
    }

    private void registerCustomer(int staffUserId) {
        System.out.println("\n--- Register Customer Profile ---");
        System.out.print("Enter linked User ID: ");
        int uid = Integer.parseInt(scanner.nextLine().trim());

        String checkUserSql = "SELECT Id FROM Users WHERE Id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkUserSql)) {
            checkPs.setInt(1, uid);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Error: User ID " + uid + " does not exist in the system. Please create the user account first.");
                    return;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking user: " + e.getMessage());
            return;
        }

        System.out.print("Full Name: "); String name = scanner.nextLine().trim();
        System.out.print("Contact Number: "); String contact = scanner.nextLine().trim();
        System.out.print("Address: "); String address = scanner.nextLine().trim();
        System.out.print("Email: "); String email = scanner.nextLine().trim();

        String sql = "INSERT INTO Customer (UserID, Fullname, Contact_Number, Address, Email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, uid); ps.setString(2, name); ps.setString(3, contact);
            ps.setString(4, address); ps.setString(5, email);
            ps.executeUpdate();
            System.out.println("Customer profile registered successfully.");
            AdminMenu.logAction(staffUserId, "REGISTER_CUSTOMER", uid, "Registered customer profile for user ID " + uid);
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void encodeLoan(int staffUserId) {
        System.out.println("\n--- Encode Credit Application ---");
        System.out.print("CustomerID: "); int cid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("EbikeID: "); int eid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Down Payment: "); double dp = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Term (Months): "); int term = Integer.parseInt(scanner.nextLine().trim());

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Verify CustomerID exists
            try (PreparedStatement cps = conn.prepareStatement("SELECT CustomerID FROM Customer WHERE CustomerID = ?")) {
                cps.setInt(1, cid);
                try (ResultSet crs = cps.executeQuery()) {
                    if (!crs.next()) {
                        System.out.println("Error: CustomerID " + cid + " does not exist. Please register the customer profile first (Option 1).");
                        return;
                    }
                }
            }

            // Verify EbikeID exists and get price
            double price = 0;
            try (PreparedStatement eps = conn.prepareStatement("SELECT Price FROM Ebike WHERE EbikeID = ?")) {
                eps.setInt(1, eid);
                try (ResultSet ers = eps.executeQuery()) {
                    if (ers.next()) {
                        price = ers.getDouble("Price");
                    } else {
                        System.out.println("Error: EbikeID " + eid + " does not exist. Check available e-bikes (Option 5).");
                        return;
                    }
                }
            }

            double monthly = (price - dp) / term;

            String insert = "INSERT INTO Loan (CustomerID, EbikeID, DownPayment, TermMonths, MonthlyAmount, Loan_Status) VALUES (?, ?, ?, ?, ?, 'Pending')";
            try (PreparedStatement ips = conn.prepareStatement(insert)) {
                ips.setInt(1, cid); ips.setInt(2, eid); ips.setDouble(3, dp);
                ips.setInt(4, term); ips.setDouble(5, monthly);
                ips.executeUpdate();
                System.out.printf("Loan encoded successfully! Status: PENDING (Waiting for Admin Approval). Monthly: %.2f\n", monthly);
                AdminMenu.logAction(staffUserId, "ENCODE_LOAN", cid, "Encoded loan application for customer ID " + cid);
            }

        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void recordPayment(int staffUserId) {
        System.out.println("\n--- Record Payment ---");
        System.out.print("LoanID: "); int lid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Amount Paid: "); double amt = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Receipt Number: "); String receipt = scanner.nextLine().trim();

        String sql = "INSERT INTO Payment (LoanID, AmountPaid, ReceiptNumber) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lid); ps.setDouble(2, amt); ps.setString(3, receipt);
            ps.executeUpdate();
            System.out.println("Payment recorded successfully. Receipt issued: " + receipt);
            AdminMenu.logAction(staffUserId, "RECORD_PAYMENT", lid, "Recorded payment of " + amt + " for loan ID " + lid);
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void viewBalances() {
        String sql = "SELECT l.LoanID, c.Fullname, (l.MonthlyAmount * l.TermMonths) AS Total, " +
                "(SELECT IFNULL(SUM(AmountPaid),0) FROM Payment p WHERE p.LoanID = l.LoanID) AS Paid, l.Loan_Status " +
                "FROM Loan l JOIN Customer c ON l.CustomerID = c.CustomerID";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nLoanID | Customer | Total Cost | Total Paid | Balance | Status");
            while (rs.next()) {
                double total = rs.getDouble("Total");
                double paid = rs.getDouble("Paid");
                System.out.printf("%d | %s | %.2f | %.2f | %.2f | %s\n",
                        rs.getInt("LoanID"), rs.getString("Fullname"), total, paid, (total - paid), rs.getString("Loan_Status"));
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void viewAvailableEbikes() {
        String sql = "SELECT * FROM Ebike WHERE Status = 'Available'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- AVAILABLE E-BIKES ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Brand: %s | Model: %s | Price: %.2f | Stock: %d\n",
                        rs.getInt("EbikeID"), rs.getString("Brand"),
                        rs.getString("Model"), rs.getDouble("Price"), rs.getInt("Stock"));
            }
            if (!found) {
                System.out.println("No available e-bikes found in inventory.");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing e-bikes: " + e.getMessage());
        }
    }
}