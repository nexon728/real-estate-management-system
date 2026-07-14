package realestate;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * CS241 – Real Estate DBMS
 * Deliverable 3 : Database Administrator CLI (JDBC)
 */
public class AdminMenu {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";

    public static void main(String[] args) {
        printBanner();

        try {
            DBConnection.getConnection();
            System.out.println(GREEN + "  [OK] Connected to real_estate_db successfully." + RESET);
        } catch (SQLException e) {
            System.out.println(RED + "  [ERROR] Cannot connect to the database: "
                    + e.getMessage() + RESET);
            System.out.println("  Please ensure MySQL is running and the credentials in");
            System.out.println("  DBConnection.java are correct, then re-run.");
            return;
        }

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print(BOLD + "  Enter choice: " + RESET);
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    // Required queries
                    case "a": case "A": QueryRunner.queryA(); break;
                    case "b": case "B": QueryRunner.queryB(); break;
                    case "c": case "C": QueryRunner.queryC(); break;
                    case "d": case "D": QueryRunner.queryD(); break;
                    case "e": case "E": QueryRunner.queryE(); break;
                    case "f": case "F": QueryRunner.queryF(); break;

                    // Additional queries
                    case "1": QueryRunner.queryBonus1(); break;
                    case "2": QueryRunner.queryBonus2(); break;
                    case "3": QueryRunner.queryBonus3(); break;
                    case "4": QueryRunner.queryBonus4(); break;
                    case "5": QueryRunner.queryBonus5(); break;
                    case "6": QueryRunner.queryBonus6(); break;

                    // Custom SQL query
                    case "q": case "Q":
                        System.out.println(YELLOW
                            + "\n  Write your SQL query. Press Enter after each line."
                            + "\n  Type  ;  on its own line (or end your last line with ;) to run."
                            + "\n  Type  /cancel  to abort."
                            + RESET);
                        StringBuilder sqlBuilder = new StringBuilder();
                        boolean cancelled = false;
                        while (true) {
                            System.out.print(CYAN + "  SQL> " + RESET);
                            String line = sc.nextLine();
                            if (line.trim().equalsIgnoreCase("/cancel")) {
                                System.out.println(RED + "  Query cancelled." + RESET);
                                cancelled = true;
                                break;
                            }
                            // If line ends with ; strip it and stop collecting
                            if (line.trim().endsWith(";")) {
                                sqlBuilder.append(" ").append(line.trim(), 0, line.trim().length() - 1);
                                break;
                            }
                            // Bare ; on its own line means done
                            if (line.trim().equals(";")) {
                                break;
                            }
                            sqlBuilder.append(" ").append(line);
                        }
                        String customSql = sqlBuilder.toString().trim();
                        if (!cancelled && !customSql.isEmpty()) {
                            QueryRunner.runCustomQuery(customSql);
                        } else if (!cancelled) {
                            System.out.println(RED + "  No query entered." + RESET);
                        }
                        break;

                    // Run all required queries
                    case "all": case "ALL":
                        System.out.println(YELLOW + "\n  Running all required queries (a-f)..." + RESET);
                        QueryRunner.queryA();
                        QueryRunner.queryB();
                        QueryRunner.queryC();
                        QueryRunner.queryD();
                        QueryRunner.queryE();
                        QueryRunner.queryF();
                        break;

                    case "0":
                        running = false;
                        System.out.println(CYAN + "\n  Exiting. Goodbye!\n" + RESET);
                        break;

                    default:
                        System.out.println(RED + "  Invalid choice. Please try again." + RESET);
                }
            } catch (SQLException ex) {
                System.out.println(RED + "  SQL Error: " + ex.getMessage() + RESET);
                System.out.println("  SQLState : " + ex.getSQLState());
                System.out.println("  ErrorCode: " + ex.getErrorCode());
            }
        }

        DBConnection.closeConnection();
        sc.close();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD);
        System.out.println("  ╔══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║         CS241 – Real Estate DBMS  (Admin Interface)         ║");
        System.out.println("  ║                Deliverable 3 – JDBC CLI                     ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private static void printMenu() {
        System.out.println();
        System.out.println(CYAN + "  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │                    QUERY MENU                               │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │  REQUIRED QUERIES                                           │");
        System.out.println("  │  a  Houses built after 2023, available for rent             │");
        System.out.println("  │  b  Avg selling price of properties sold in 2018            │");
        System.out.println("  │  c  Rent on GS Road: >=2 beds, price < Rs.15,000           │");
        System.out.println("  │  d  Top-selling agent in 2023                               │");
        System.out.println("  │  e  Avg days on market per agent (2018 sales)               │");
        System.out.println("  │  f  Most expensive house (sale) & highest rent house        │");
        System.out.println("  │  all  Run all required queries (a-f)                        │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │  ADDITIONAL QUERIES                                         │");
        System.out.println("  │  1  Houses priced Rs.20L - Rs.60L (Guwahati)               │");
        System.out.println("  │  2  All currently active listings                           │");
        System.out.println("  │  3  Full transaction history                                │");
        System.out.println("  │  4  Agent performance summary                               │");
        System.out.println("  │  5  Current property ownership                              │");
        System.out.println("  │  6  Active leases & rent details                            │");
        System.out.println("  ├─────────────────────────────────────────────────────────────┤");
        System.out.println("  │  q  Custom SQL query (multiline, end with ;)                │");
        System.out.println("  │  0  Exit                                                    │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        System.out.println(RESET);
    }
}
