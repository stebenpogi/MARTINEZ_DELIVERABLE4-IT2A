package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Guardrail: Ensure database and tables are ready before starting CLI
        if (!DatabaseInitializer.initialize()) {
            System.err.println("Cannot start: database is not ready.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();

        // Delegate execution to Authentication class using shared resources
        new Authentication(scanner, userDao).start();

        scanner.close();
    }
}