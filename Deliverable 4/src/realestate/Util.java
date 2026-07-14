package realestate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Scanner;

/**
 * Shared terminal-formatting utilities used by both Office and Agent interfaces.
 */
public class Util {

    // ── ANSI colours ─────────────────────────────────────────────────────────
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String DIM    = "\u001B[2m";
    public static final String CYAN   = "\u001B[36m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED    = "\u001B[31m";
    public static final String BLUE   = "\u001B[34m";
    public static final String WHITE  = "\u001B[37m";
    public static final String MAGENTA = "\u001B[35m";

    private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));

    // ── Section header ───────────────────────────────────────────────────────
    /** Print a section header box. */
    public static void header(String title) {
        System.out.println();
        int w = 64;
        String line = "─".repeat(w);
        System.out.println(CYAN + BOLD + "  ┌" + line + "┐");
        System.out.printf("  │  %-" + (w - 2) + "s  │%n", title);
        System.out.println("  └" + line + "┘" + RESET);
    }

    /** Print a sub-header (no box, just bold text). */
    public static void subHeader(String title) {
        System.out.println();
        System.out.println("  " + BOLD + YELLOW + "── " + title + " ──" + RESET);
    }

    /** Print a visual separator line. */
    public static void separator() {
        System.out.println("  " + DIM + "─".repeat(64) + RESET);
    }

    /** Print a simple labelled value. */
    public static void kv(String label, String value) {
        System.out.printf("  " + BOLD + "%-28s" + RESET + "%s%n", label + ":", value);
    }

    // ── ResultSet table printer ──────────────────────────────────────────────
    /** Auto-print any ResultSet as a formatted table. */
    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        String[] colNames = new String[cols];
        int[]    widths   = new int[cols];
        for (int i = 0; i < cols; i++) {
            colNames[i] = meta.getColumnLabel(i + 1);
            widths[i]   = Math.max(colNames[i].length(), 10);
        }

        // Collect rows & widen columns
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 0; i < cols; i++) {
                String v = rs.getString(i + 1);
                row[i]   = (v == null) ? "NULL" : v;
                widths[i] = Math.min(32, Math.max(widths[i], row[i].length()));
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            System.out.println();
            info("No records found.");
            System.out.println();
            return;
        }

        // Print header
        System.out.println();
        printRow(colNames, widths, true);
        printDivider(widths);
        for (String[] row : rows) printRow(row, widths, false);
        System.out.printf("%n  " + GREEN + "[%d row(s)]" + RESET + "%n%n", rows.size());
    }

    private static void printRow(String[] vals, int[] widths, boolean isHeader) {
        StringBuilder sb = new StringBuilder("  ");
        if (isHeader) sb.insert(0, BOLD + BLUE);
        for (int i = 0; i < vals.length; i++) {
            String v = (vals[i] == null) ? "NULL" : vals[i];
            if (v.length() > widths[i]) v = v.substring(0, widths[i] - 1) + "…";
            sb.append(String.format("%-" + widths[i] + "s", v));
            if (i < vals.length - 1) sb.append("  ");
        }
        if (isHeader) sb.append(RESET);
        System.out.println(sb);
    }

    private static void printDivider(int[] widths) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < widths.length; i++) {
            sb.append("─".repeat(widths[i]));
            if (i < widths.length - 1) sb.append("  ");
        }
        System.out.println(sb);
    }

    // ── Message helpers ──────────────────────────────────────────────────────
    public static void success(String msg) {
        System.out.println("  " + GREEN + "✔  " + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println("  " + RED + "✘  " + msg + RESET);
    }

    public static void info(String msg) {
        System.out.println("  " + YELLOW + "ℹ  " + msg + RESET);
    }

    public static void warn(String msg) {
        System.out.println("  " + MAGENTA + "⚠  " + msg + RESET);
    }

    public static void prompt(String msg) {
        System.out.print("  " + BOLD + msg + RESET);
    }

    // ── Input helpers (with validation) ──────────────────────────────────────

    /**
     * Read a positive integer from the scanner. Returns -1 on invalid input.
     * Prints an error message if input is not a valid positive integer.
     */
    public static int readPositiveInt(Scanner sc, String promptMsg) {
        prompt(promptMsg);
        String line = sc.nextLine().trim();
        try {
            int val = Integer.parseInt(line);
            if (val < 0) {
                error("Value cannot be negative.");
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            error("Invalid number. Please enter a valid integer.");
            return -1;
        }
    }

    /**
     * Read an integer allowing 0 as cancel. Returns -1 on invalid input.
     */
    public static int readIntOrCancel(Scanner sc, String promptMsg) {
        prompt(promptMsg);
        String line = sc.nextLine().trim();
        try {
            int val = Integer.parseInt(line);
            if (val < 0) {
                error("Value cannot be negative.");
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            error("Invalid number. Please enter a valid integer.");
            return -1;
        }
    }

    /**
     * Read a positive double value. Returns -1.0 on invalid input.
     */
    public static double readPositiveDouble(Scanner sc, String promptMsg) {
        prompt(promptMsg);
        String line = sc.nextLine().trim();
        try {
            double val = Double.parseDouble(line);
            if (val <= 0) {
                error("Value must be greater than zero.");
                return -1.0;
            }
            return val;
        } catch (NumberFormatException e) {
            error("Invalid number. Please enter a valid amount.");
            return -1.0;
        }
    }

    /**
     * Read a date string and validate YYYY-MM-DD format.
     * Returns null on invalid input.
     */
    public static String readDate(Scanner sc, String promptMsg) {
        prompt(promptMsg);
        String line = sc.nextLine().trim();
        if (!line.matches("\\d{4}-\\d{2}-\\d{2}")) {
            error("Invalid date format. Use YYYY-MM-DD (e.g. 2024-06-15).");
            return null;
        }
        // Basic range validation
        try {
            String[] parts = line.split("-");
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day   = Integer.parseInt(parts[2]);
            if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31) {
                error("Date values out of range.");
                return null;
            }
        } catch (NumberFormatException e) {
            error("Invalid date.");
            return null;
        }
        return line;
    }

    /**
     * Ask for yes/no confirmation. Returns true if user types y/yes.
     */
    public static boolean confirm(Scanner sc, String msg) {
        prompt(msg + " (y/n): ");
        String line = sc.nextLine().trim().toLowerCase();
        return line.equals("y") || line.equals("yes");
    }

    /**
     * Format a number as Indian currency string.
     */
    public static String formatCurrency(double amount) {
        return "Rs. " + CURRENCY_FMT.format(amount);
    }

    /**
     * Format a number as Indian currency string from a string representation.
     */
    public static String formatCurrency(String amount) {
        try {
            return formatCurrency(Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            return "Rs. " + amount;
        }
    }
}
