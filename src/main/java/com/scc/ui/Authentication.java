package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.Scanner;

public class Authentication {
    private final Scanner scanner;
    private final UserDao userDao;
    private final AdminMenu adminMenu;
    private final StaffMenu staffMenu;
    private final CustomerMenu customerMenu;

    public Authentication(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
        // Pass shared single instances of UserDao and Scanner across all menus
        this.adminMenu = new AdminMenu(scanner, userDao);
        this.staffMenu = new StaffMenu(scanner, userDao);
        this.customerMenu = new CustomerMenu(scanner, userDao);
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n==============================");
            System.out.println(" E-BIKE CREDIT MANAGEMENT");
            System.out.println("==============================");
            System.out.println("1. LogIn");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print(" CHOOSE AN OPTION: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void register() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine().trim();
        String role = askRole();
        if (role == null) return;
        userDao.registerUser(username, password, role);
    }

    private void login() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine().trim();
        String role = askRole();
        if (role == null) return;

        User user = userDao.findForLogin(username, password, role);
        if (user == null) {
            System.out.println("Login failed. Check your credentials and role.");
            return;
        }
        if (user.Status().equalsIgnoreCase("PENDING")) {
            System.out.println("Your account is still waiting for admin approval.");
            return;
        }
        if (!user.Status().equalsIgnoreCase("APPROVED")) {
            System.out.println("Your account is " + user.Status() + ".");
            return;
        }

        System.out.println("Welcome, " + user.Username() + "!");
        routeByRole(user);
    }

    private void routeByRole(User user) {
        switch (user.Roles().toUpperCase()) {
            case "ADMIN"    -> adminMenu.display(user);
            case "STAFF"    -> staffMenu.display(user);
            case "CUSTOMER" -> customerMenu.display(user);
            default         -> System.out.println("Unknown role: " + user.Roles());
        }
    }

    private String askRole() {
        System.out.println("Select Role:");
        System.out.println("  [1] ADMIN");
        System.out.println("  [2] STAFF");
        System.out.println("  [3] CUSTOMER");
        System.out.print("Role: ");
        return switch (scanner.nextLine().trim()) {
            case "1" -> "ADMIN";
            case "2" -> "STAFF";
            case "3" -> "CUSTOMER";
            default -> { System.out.println("Invalid role."); yield null; }
        };
    }
}