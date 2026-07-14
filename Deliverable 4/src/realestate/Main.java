package realestate;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * CS241 – Real Estate DBMS
 * Deliverable 4 — Interfaces for Real Estate Office & Agents
 *
 * Compile:
 *   javac -cp ".;..\lib\mysql-connector-j-9.6.0.jar" -d out src\realestate\*.java
 *
 * Run:
 *   java -cp ".;..\lib\mysql-connector-j-9.6.0.jar;out" realestate.Main
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        // Verify DB connection once at startup
        try {
            DBConnection.getConnection();
            System.out.println(Util.GREEN
                + "  [OK] Connected to real_estate_db successfully." + Util.RESET);
        } catch (SQLException e) {
            System.out.println(Util.RED
                + "  [ERROR] Cannot connect to database: " + e.getMessage() + Util.RESET);
            System.out.println("  Check MySQL is running and credentials in DBConnection.java.");
            return;
        }

        Scanner sc      = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            Util.prompt("Enter choice: ");
            String ch = sc.nextLine().trim();
            System.out.println();

            switch (ch) {
                case "1":
                    new OfficeReports(sc).run();
                    break;
                case "2":
                    new AgentInterface(sc).run();
                    break;
                case "3":
                    printAbout();
                    break;
                case "0":
                    running = false;
                    printExitBanner();
                    break;
                default:
                    Util.error("Invalid choice. Please enter 0, 1, 2, or 3.");
            }
        }

        DBConnection.closeConnection();
        sc.close();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(Util.CYAN + Util.BOLD);
        System.out.println("  ╔══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║         CS241 Real Estate Management System                  ║");
        System.out.println("  ║              Deliverable 4  Office & Agent CLI               ║");
        System.out.println("  ╠══════════════════════════════════════════════════════════════╣");
        System.out.println("  ║  Features:                                                  ║");
        System.out.println("  ║    • Office Reports — Sales, Rentals & Agent Performance    ║");
        System.out.println("  ║    • Agent Portal   — Record Sales, Rentals & Listings      ║");
        System.out.println("  ║    • Property Search — By Locality, Type & Availability     ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
        System.out.println(Util.RESET);
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println(Util.CYAN
            + "  ┌──────────────────────────────────────────────────────────────┐"
            + "\n  │                     MAIN MENU                               │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  1  Real Estate Office  (reports, search & summaries)       │"
            + "\n  │  2  Agent Portal        (record sales, rentals & listings)  │"
            + "\n  │  3  About               (team & contributions)              │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  0  Exit                                                    │"
            + "\n  └──────────────────────────────────────────────────────────────┘"
            + Util.RESET);
    }

    private static void printAbout() {
        System.out.println();
        System.out.println(Util.CYAN + Util.BOLD);
        System.out.println("  ╔══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║                        ABOUT                                ║");
        System.out.println("  ║          CS241 – Real Estate Management System              ║");
        System.out.println("  ║              Deliverable 4 – Office & Agent CLI             ║");
        System.out.println("  ╠══════════════════════════════════════════════════════════════╣");
        System.out.println("  ║  Team Members & Contributions:                              ║");
        System.out.println("  ║                                                              ║");
        System.out.println("  ║  1. Member 1 (Prabhat Singh - 2401143.) — ER Diagram,        ║");
        System.out.println("  ║      Schema Design, Data Population, Triggers                ║");
        System.out.println("  ║                                                              ║");
        System.out.println("  ║  2. Member 2 (Pradyumn Ojha - 2401145) —                     ║");
        System.out.println("  ║     Office Interface (Reports),  Sales & Rental Queries      ║");
        System.out.println("  ║                                                              ║");
        System.out.println("  ║  3. Member 3 (Vighnesh Mani Tripathi - 2401230)              ║");
        System.out.println("  ║ Agent Interface, Transaction Recording, Listing Management   ║");
        System.out.println("  ║                                                              ║");
        // System.out.println("  ║  4. Member 4 (Roll No.) — JDBC Integration,                 ║");
        // System.out.println("  ║     Testing, Utility Functions, Documentation                ║");
        System.out.println("  ╠══════════════════════════════════════════════════════════════╣");
        System.out.println("  ║  Database: MySQL 8.0  |  Language: Java (JDBC)              ║");
        System.out.println("  ║  Interface: Command-Line (CLI)                               ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
        System.out.println(Util.RESET);
    }

    private static void printExitBanner() {
        System.out.println();
        System.out.println(Util.CYAN + Util.BOLD);
        System.out.println("  ╔══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║              Thank you for using the system!                ║");
        System.out.println("  ║                      Goodbye! 👋                            ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
        System.out.println(Util.RESET);
    }
}
