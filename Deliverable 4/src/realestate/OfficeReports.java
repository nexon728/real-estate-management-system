package realestate;

import java.sql.*;
import java.util.Scanner;

/**
 * CS241 Deliverable 4 — Interface for the Real Estate Office.
 *
 * Provides:
 *   1. Sales report per agent  (sale dates, property details, selling price)
 *   2. Rental report per agent (properties rented, amount, area, when)
 *   3. Overall sales summary   (all agents ranked by total revenue)
 *   4. Overall rental summary  (all agents ranked by properties rented)
 *   5. Active listings report  (what is currently on the market)
 *   6. Transaction history     (full log of all completed transactions)
 *   7. View all properties     (complete property inventory)
 *   8. Available properties    (unsold / unrented properties)
 *   9. Search property by locality
 *  10. Agent-wise performance summary
 */
public class OfficeReports {

    private final Scanner sc;

    public OfficeReports(Scanner sc) { this.sc = sc; }

    // ── MENU ──────────────────────────────────────────────────────────────────
    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            Util.prompt("Enter choice: ");
            String ch = sc.nextLine().trim();
            System.out.println();
            try {
                switch (ch) {
                    case "1":  salesReportByAgent();        break;
                    case "2":  rentalReportByAgent();       break;
                    case "3":  allAgentSalesSummary();      break;
                    case "4":  allAgentRentalSummary();     break;
                    case "5":  activeListingsReport();      break;
                    case "6":  fullTransactionHistory();    break;
                    case "7":  viewAllProperties();         break;
                    case "8":  viewAvailableProperties();   break;
                    case "9":  searchPropertyByLocality();  break;
                    case "10": agentPerformanceSummary();   break;
                    case "11": requiredDemoQueries();       break;
                    case "12": propertyOwnershipHistory();   break;
                    case "13": registerNewAgent();            break;
                    case "0":  back = true;                 break;
                    default:   Util.error("Invalid choice. Please enter 0-13."); break;
                }
            } catch (SQLException e) {
                Util.error("SQL Error: " + e.getMessage());
                System.out.println("  " + Util.DIM + "SQLState: " + e.getSQLState()
                    + " | ErrorCode: " + e.getErrorCode() + Util.RESET);
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println(Util.CYAN
            + "  ┌──────────────────────────────────────────────────────────────┐"
            + "\n  │          REAL ESTATE OFFICE — REPORTS MENU                  │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │                 AGENT REPORTS                               │"
            + "\n  │  1   Sales report per agent                                 │"
            + "\n  │  2   Rental report per agent                                │"
            + "\n  │  3   All-agent sales summary (ranked by revenue)            │"
            + "\n  │  4   All-agent rental summary (ranked by count)             │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │                 LISTINGS & TRANSACTIONS                     │"
            + "\n  │  5   Active listings report                                 │"
            + "\n  │  6   Full transaction history                               │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │                 PROPERTY SEARCH & VIEWS                     │"
            + "\n  │  7   View all properties                                    │"
            + "\n  │  8   View available (unsold/unrented) properties            │"
            + "\n  │  9   Search property by locality                            │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │                 PERFORMANCE & DEMO                          │"
            + "\n  │  10  Agent-wise performance summary                         │"
            + "\n  │  11  Required Demo Queries (from PDF a-f)                   │"
            + "\n  │  12  Property Ownership History                              │"
            + "\n  │  13  Register a new agent                                   │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  0   Back to main menu                                      │"
            + "\n  └──────────────────────────────────────────────────────────────┘"
            + Util.RESET);
    }

    // ── 1. Sales report for a specific agent ─────────────────────────────────
    private void salesReportByAgent() throws SQLException {
        listAgents();
        int agentId = Util.readIntOrCancel(sc, "Enter Agent ID (or 0 to cancel): ");
        if (agentId <= 0) return;

        if (!agentExists(agentId)) {
            Util.error("Agent ID " + agentId + " does not exist in the system.");
            return;
        }

        String sql =
            "SELECT t.transaction_date AS sale_date, " +
            "       p.property_id, p.address, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       l.listed_price, t.final_amount AS selling_price, " +
            "       pe.first_name AS buyer_fn, pe.last_name AS buyer_ln " +
            "FROM transaction_ AS t " +
            "JOIN listing  AS l  ON t.listing_id  = l.listing_id " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN person   AS pe ON t.customer_id = pe.person_id " +
            "WHERE l.agent_id = ? " +
            "  AND l.type = 'sale' " +
            "ORDER BY t.transaction_date";

        Util.header("Sales Report - Agent ID " + agentId);
        printAgentName(agentId);

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }
        printAgentSaleTotals(agentId);
    }

    // ── 2. Rental report for a specific agent ────────────────────────────────
    private void rentalReportByAgent() throws SQLException {
        listAgents();
        int agentId = Util.readIntOrCancel(sc, "Enter Agent ID (or 0 to cancel): ");
        if (agentId <= 0) return;

        if (!agentExists(agentId)) {
            Util.error("Agent ID " + agentId + " does not exist in the system.");
            return;
        }

        String sql =
            "SELECT t.transaction_date AS rental_date, " +
            "       p.property_id, p.address, p.area, p.property_type, " +
            "       p.bedrooms, " +
            "       rd.monthly_rent, rd.lease_start, rd.lease_end, " +
            "       pe.first_name AS tenant_fn, pe.last_name AS tenant_ln " +
            "FROM transaction_ AS t " +
            "JOIN listing     AS l  ON t.listing_id      = l.listing_id " +
            "JOIN property    AS p  ON l.property_id     = p.property_id " +
            "JOIN rent_detail AS rd ON rd.transaction_id = t.transaction_id " +
            "JOIN person      AS pe ON t.customer_id     = pe.person_id " +
            "WHERE l.agent_id = ? " +
            "  AND l.type = 'rent' " +
            "ORDER BY t.transaction_date";

        Util.header("Rental Report — Agent ID " + agentId);
        printAgentName(agentId);

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }
        printAgentRentalTotals(agentId);
    }

    // ── 3. All-agent sales summary ────────────────────────────────────────────
    private void allAgentSalesSummary() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       COUNT(t.transaction_id) AS properties_sold, " +
            "       COALESCE(SUM(t.final_amount), 0) AS total_revenue, " +
            "       COALESCE(AVG(t.final_amount), 0) AS avg_sale_price, " +
            "       ROUND(a.commission_percent, 2) AS commission_pct " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "LEFT JOIN listing      AS l ON a.agent_id   = l.agent_id AND l.type = 'sale' " +
            "LEFT JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name, a.commission_percent " +
            "ORDER BY total_revenue DESC";

        Util.header("All-Agent Sales Summary (ranked by total revenue)");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── 4. All-agent rental summary ───────────────────────────────────────────
    private void allAgentRentalSummary() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       COUNT(t.transaction_id) AS properties_rented, " +
            "       COALESCE(SUM(rd.monthly_rent), 0) AS total_monthly_rent, " +
            "       GROUP_CONCAT(DISTINCT p.area ORDER BY p.area SEPARATOR ', ') AS areas " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "LEFT JOIN listing      AS l  ON a.agent_id      = l.agent_id AND l.type = 'rent' " +
            "LEFT JOIN transaction_ AS t  ON l.listing_id    = t.listing_id " +
            "LEFT JOIN rent_detail  AS rd ON rd.transaction_id = t.transaction_id " +
            "LEFT JOIN property     AS p  ON l.property_id   = p.property_id " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name " +
            "ORDER BY properties_rented DESC";

        Util.header("All-Agent Rental Summary (ranked by count)");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── 5. Active listings report ─────────────────────────────────────────────
    private void activeListingsReport() throws SQLException {
        String sql =
            "SELECT l.listing_id, l.type AS listing_type, l.listed_price, " +
            "       l.list_date, " +
            "       p.property_id, p.address, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       pe.first_name AS agent_fn, pe.last_name AS agent_ln " +
            "FROM listing  AS l " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN agent    AS a  ON l.agent_id    = a.agent_id " +
            "JOIN person   AS pe ON a.person_id   = pe.person_id " +
            "WHERE l.status = 'active' " +
            "ORDER BY l.type, l.listed_price DESC";

        Util.header("Active Listings Report");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }

        // Print summary counts
        String countSql =
            "SELECT " +
            "  COUNT(CASE WHEN type = 'sale' THEN 1 END) AS sale_listings, " +
            "  COUNT(CASE WHEN type = 'rent' THEN 1 END) AS rent_listings, " +
            "  COUNT(*) AS total " +
            "FROM listing WHERE status = 'active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Util.separator();
                Util.kv("  Active sale listings", rs.getString("sale_listings"));
                Util.kv("  Active rent listings", rs.getString("rent_listings"));
                Util.kv("  Total active",         rs.getString("total"));
                System.out.println();
            }
        }
    }

    // ── 6. Full transaction history ───────────────────────────────────────────
    private void fullTransactionHistory() throws SQLException {
        String sql =
            "SELECT t.transaction_id, t.transaction_date, l.type AS txn_type, " +
            "       t.final_amount, " +
            "       p.address, p.area, " +
            "       ag.first_name AS agent_fn, ag.last_name AS agent_ln, " +
            "       pe.first_name AS customer_fn, pe.last_name AS customer_ln " +
            "FROM transaction_ AS t " +
            "JOIN listing  AS l  ON t.listing_id  = l.listing_id " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN agent    AS a  ON l.agent_id    = a.agent_id " +
            "JOIN person   AS ag ON a.person_id   = ag.person_id " +
            "JOIN person   AS pe ON t.customer_id = pe.person_id " +
            "ORDER BY t.transaction_date";

        Util.header("Full Transaction History");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }

        // Summary
        String sumSql =
            "SELECT COUNT(*) AS total_txns, " +
            "  COUNT(CASE WHEN l.type='sale' THEN 1 END) AS sale_txns, " +
            "  COUNT(CASE WHEN l.type='rent' THEN 1 END) AS rent_txns, " +
            "  COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END), 0) AS total_sale_value " +
            "FROM transaction_ AS t JOIN listing AS l ON t.listing_id = l.listing_id";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sumSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Util.separator();
                Util.kv("  Total transactions", rs.getString("total_txns"));
                Util.kv("  Sale transactions",  rs.getString("sale_txns"));
                Util.kv("  Rent transactions",  rs.getString("rent_txns"));
                Util.kv("  Total sale value",    Util.formatCurrency(rs.getString("total_sale_value")));
                System.out.println();
            }
        }
    }

    // ── 7. View all properties ────────────────────────────────────────────────
    private void viewAllProperties() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.city, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       COALESCE(pe.first_name, '-') AS owner_fn, " +
            "       COALESCE(pe.last_name, '') AS owner_ln " +
            "FROM property AS p " +
            "LEFT JOIN ownership AS o ON p.property_id = o.property_id AND o.end_date IS NULL " +
            "LEFT JOIN person AS pe ON o.owner_id = pe.person_id " +
            "ORDER BY p.property_id";

        Util.header("All Properties (complete inventory)");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }

        // Property type breakdown
        String breakSql =
            "SELECT property_type, COUNT(*) AS count, " +
            "       ROUND(AVG(size_sqft)) AS avg_size, ROUND(AVG(bedrooms),1) AS avg_bedrooms " +
            "FROM property GROUP BY property_type";
        Util.subHeader("Property Type Breakdown");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(breakSql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── 8. Available (unsold/unrented) properties ─────────────────────────────
    private void viewAvailableProperties() throws SQLException {
        // Properties that currently have an active listing OR have no listing at all
        String sql =
            "SELECT p.property_id, p.address, p.city, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       CASE WHEN l.listing_id IS NOT NULL THEN CONCAT(l.type, ' @ Rs.', l.listed_price) " +
            "            ELSE 'Not Listed' END AS listing_info " +
            "FROM property AS p " +
            "LEFT JOIN listing AS l ON p.property_id = l.property_id AND l.status = 'active' " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 FROM listing AS l2 " +
            "    WHERE l2.property_id = p.property_id " +
            "      AND l2.status IN ('sold', 'rented') " +
            ") " +
            "OR l.status = 'active' " +
            "ORDER BY p.property_id";

        Util.header("Available Properties (unsold/unrented or actively listed)");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── 9. Search property by locality ────────────────────────────────────────
    private void searchPropertyByLocality() throws SQLException {
        // First show available localities
        showAvailableLocalities();

        Util.prompt("Enter area/locality to search (e.g. GS Road, Beltola): ");
        String area = sc.nextLine().trim();
        if (area.isEmpty()) {
            Util.error("Area cannot be empty.");
            return;
        }

        String sql =
            "SELECT p.property_id, p.address, p.city, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       COALESCE(l.status, 'No listing') AS listing_status, " +
            "       COALESCE(l.type, '-') AS listing_type, " +
            "       COALESCE(CAST(l.listed_price AS CHAR), '-') AS listed_price " +
            "FROM property AS p " +
            "LEFT JOIN listing AS l ON p.property_id = l.property_id " +
            "  AND l.listing_id = (SELECT MAX(l2.listing_id) FROM listing l2 WHERE l2.property_id = p.property_id) " +
            "WHERE p.area LIKE ? " +
            "ORDER BY p.property_id";

        Util.header("Property Search - Locality: " + area);
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + area + "%");
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }
    }

    // ── 10. Agent-wise performance summary ────────────────────────────────────
    private void agentPerformanceSummary() throws SQLException {
        String sql =
            "SELECT a.agent_id, " +
            "       pe.first_name, pe.last_name, " +
            "       a.experience_years AS exp_yrs, " +
            "       a.commission_percent AS comm_pct, " +
            "       COUNT(DISTINCT CASE WHEN l.type='sale' AND t.transaction_id IS NOT NULL THEN t.transaction_id END) AS sales, " +
            "       COUNT(DISTINCT CASE WHEN l.type='rent' AND t.transaction_id IS NOT NULL THEN t.transaction_id END) AS rentals, " +
            "       COUNT(DISTINCT CASE WHEN l.status='active' THEN l.listing_id END) AS active_listings, " +
            "       COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END), 0) AS sale_revenue, " +
            "       COALESCE(SUM(CASE WHEN l.type='rent' THEN t.final_amount END), 0) AS rent_revenue, " +
            "       ROUND(COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END), 0) " +
            "             * a.commission_percent / 100, 2) AS est_commission " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "LEFT JOIN listing      AS l ON a.agent_id   = l.agent_id " +
            "LEFT JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name, " +
            "         a.experience_years, a.commission_percent " +
            "ORDER BY sale_revenue DESC, rentals DESC";

        Util.header("Agent-wise Performance Summary");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }

        // Top performer highlight
        String topSql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       COALESCE(SUM(t.final_amount), 0) AS total_rev " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "LEFT JOIN listing AS l ON a.agent_id = l.agent_id AND l.type = 'sale' " +
            "LEFT JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name " +
            "ORDER BY total_rev DESC LIMIT 1";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(topSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getDouble("total_rev") > 0) {
                Util.separator();
                System.out.println("  " + Util.GREEN + Util.BOLD + " TOP PERFORMER: "
                    + rs.getString("first_name") + " " + rs.getString("last_name")
                    + " (Agent #" + rs.getInt("agent_id") + ")"
                    + " - Total Sale Revenue: " + Util.formatCurrency(rs.getDouble("total_rev"))
                    + Util.RESET);
                System.out.println();
            }
        }
    }

    // ── 11. Required Demo Queries (from PDF a-f) ──────────────────────────────
    private void requiredDemoQueries() throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println(Util.CYAN
                + "  ┌──────────────────────────────────────────────────────────────┐"
                + "\n  │                 REQUIRED DEMO QUERIES                       │"
                + "\n  ├──────────────────────────────────────────────────────────────┤"
                + "\n  │  a) Houses > 2023 available for rent                        │"
                + "\n  │  b) Houses costing between Rs.20L and Rs.60L                │"
                + "\n  │  c) Rent in GS Road, >=2 beds, <Rs.15,000/month             │"
                + "\n  │  d) Agent who sold most property in 2023 (by revenue)       │"
                + "\n  │  e) Agent averages (price & days on market) for 2018        │"
                + "\n  │  f) Most expensive houses & highest rent properties         │"
                + "\n  │  g) Run a custom SQL query                                  │"
                + "\n  ├──────────────────────────────────────────────────────────────┤"
                + "\n  │  0) Back to Office Menu                                     │"
                + "\n  └──────────────────────────────────────────────────────────────┘"
                + Util.RESET);
            
            Util.prompt("Enter query letter (a-g) or 0: ");
            String ch = sc.nextLine().trim().toLowerCase();
            System.out.println();

            try {
                switch (ch) {
                    case "a": queryA(); break;
                    case "b": queryB(); break;
                    case "c": queryC(); break;
                    case "d": queryD(); break;
                    case "e": queryE(); break;
                    case "f": queryF(); break;
                    case "g": runCustomQuery(); break;
                    case "0": back = true; break;
                    default: Util.error("Invalid choice."); break;
                }
            } catch (SQLException e) {
                Util.error("SQL Error: " + e.getMessage());
            }
        }
    }

    private void queryA() throws SQLException {
        Util.header("Query (a): Houses > 2023 available for rent");
        String sql = "SELECT p.property_id, p.address, p.city, p.property_type, p.year_built, l.listed_price " +
                     "FROM property p JOIN listing l ON p.property_id = l.property_id " +
                     "WHERE p.year_built > 2023 AND l.type = 'rent' AND l.status = 'active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void queryB() throws SQLException {
        Util.header("Query (b): Houses costing between 20L and 60L");
        String sql = "SELECT p.property_id, p.address, p.city, l.listed_price " +
                     "FROM property p JOIN listing l ON p.property_id = l.property_id " +
                     "WHERE l.type = 'sale' AND l.listed_price BETWEEN 2000000 AND 6000000";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void queryC() throws SQLException {
        Util.header("Query (c): GS Road, >=2 beds, rent < 15,000");
        String sql = "SELECT p.property_id, p.address, p.area, p.bedrooms, l.listed_price AS rent " +
                     "FROM property p JOIN listing l ON p.property_id = l.property_id " +
                     "WHERE p.area = 'GS Road' AND p.bedrooms >= 2 AND l.type = 'rent' AND l.listed_price < 15000 AND l.status = 'active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void queryD() throws SQLException {
        Util.header("Query (d): Top agent in 2023 (by total amount)");
        String sql = "SELECT a.agent_id, pe.first_name, pe.last_name, SUM(t.final_amount) AS total_sold_amount " +
                     "FROM agent a JOIN person pe ON a.person_id = pe.person_id " +
                     "JOIN listing l ON a.agent_id = l.agent_id " +
                     "JOIN transaction_ t ON l.listing_id = t.listing_id " +
                     "WHERE l.type = 'sale' AND YEAR(t.transaction_date) = 2023 " +
                     "GROUP BY a.agent_id, pe.first_name, pe.last_name " +
                     "ORDER BY total_sold_amount DESC LIMIT 1";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void queryE() throws SQLException {
        Util.header("Query (e): Agent averages for 2018 (price & days on market)");
        String sql = "SELECT a.agent_id, pe.first_name, pe.last_name, " +
                     "ROUND(AVG(t.final_amount), 2) AS avg_sale_price, " +
                     "ROUND(AVG(DATEDIFF(t.transaction_date, l.list_date)), 1) AS avg_days_on_market " +
                     "FROM agent a JOIN person pe ON a.person_id = pe.person_id " +
                     "JOIN listing l ON a.agent_id = l.agent_id " +
                     "JOIN transaction_ t ON l.listing_id = t.listing_id " +
                     "WHERE l.type = 'sale' AND YEAR(t.transaction_date) = 2018 " +
                     "GROUP BY a.agent_id, pe.first_name, pe.last_name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void queryF() throws SQLException {
        Util.header("Query (f): Most expensive houses & highest rent properties");
        System.out.println("  " + Util.YELLOW + "Most Expensive Houses (Sale):" + Util.RESET);
        String sqlSale = "SELECT p.property_id, p.address, p.property_type, l.listed_price " +
                         "FROM property p JOIN listing l ON p.property_id = l.property_id " +
                         "WHERE l.type = 'sale' ORDER BY l.listed_price DESC LIMIT 5";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlSale);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
             
        System.out.println("  " + Util.YELLOW + "Highest Rent Properties:" + Util.RESET);
        String sqlRent = "SELECT p.property_id, p.address, p.property_type, l.listed_price AS rent " +
                         "FROM property p JOIN listing l ON p.property_id = l.property_id " +
                         "WHERE l.type = 'rent' ORDER BY l.listed_price DESC LIMIT 5";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlRent);
             ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
    }

    private void runCustomQuery() {
    Util.header("Custom SQL Query");
    System.out.println("  " + Util.DIM + "Type your query. End with ';' on a new line or type 'cancel'." + Util.RESET);
    Util.prompt("Enter SQL:\n");
    
    StringBuilder sb = new StringBuilder();
    while (true) {
        System.out.print("  > ");
        String line = sc.nextLine().trim();
        if (line.equalsIgnoreCase("cancel")) return;
        if (line.equals(";")) break;
        if (line.endsWith(";")) {
            sb.append(line, 0, line.length() - 1);
            break;
        }
        sb.append(" ").append(line);
    }

    String sql = sb.toString().trim();
    if (sql.isEmpty()) return;

    try (Statement stmt = DBConnection.getConnection().createStatement()) {
        boolean isResultSet = stmt.execute(sql);
        if (isResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
                Util.printResultSet(rs);
            }
        } else {
            int rows = stmt.getUpdateCount();
            Util.success("Query executed successfully. Rows affected: " + rows);
        }
    } catch (SQLException e) {
        Util.error("Error executing custom query: " + e.getMessage());
    }
}

    // ── 12. Property Ownership History ─────────────────────────────────────────
    private void propertyOwnershipHistory() throws SQLException {
        Util.header("Property Ownership History");

        // Show all properties for reference
        String listSql =
            "SELECT p.property_id, p.address, p.area, p.property_type " +
            "FROM property p ORDER BY p.property_id";
        Util.info("Available properties:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(listSql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }

        int propId = Util.readIntOrCancel(sc, "Enter Property ID (or 0 to cancel): ");
        if (propId <= 0) return;

        // Verify property exists and show its details
        String detailSql =
            "SELECT property_id, address, city, area, property_type, size_sqft, bedrooms, year_built " +
            "FROM property WHERE property_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(detailSql)) {
            ps.setInt(1, propId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Util.error("Property ID " + propId + " does not exist.");
                    return;
                }
                Util.subHeader("Property Details");
                Util.kv("  Property ID",  String.valueOf(rs.getInt("property_id")));
                Util.kv("  Address",      rs.getString("address") + ", " + rs.getString("area") + ", " + rs.getString("city"));
                Util.kv("  Type",         rs.getString("property_type"));
                Util.kv("  Size",         rs.getInt("size_sqft") + " sq.ft, " + rs.getInt("bedrooms") + " bed(s)");
                Util.kv("  Year Built",   String.valueOf(rs.getInt("year_built")));
            }
        }

        // Fetch ownership history
        String sql =
            "SELECT o.ownership_id, " +
            "       pe.person_id, pe.first_name, pe.last_name, pe.email, " +
            "       o.start_date, o.end_date, " +
            "       CASE WHEN o.end_date IS NULL THEN 'CURRENT OWNER' ELSE 'Previous Owner' END AS status " +
            "FROM ownership o " +
            "JOIN person pe ON o.owner_id = pe.person_id " +
            "WHERE o.property_id = ? " +
            "ORDER BY o.start_date ASC";

        Util.subHeader("Ownership History (oldest to newest)");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, propId);
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }

        // Count summary
        String countSql =
            "SELECT COUNT(*) AS total_owners, " +
            "       MIN(o.start_date) AS first_owned_since " +
            "FROM ownership o WHERE o.property_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(countSql)) {
            ps.setInt(1, propId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("total_owners") > 0) {
                    Util.separator();
                    Util.kv("  Total owners over time", rs.getString("total_owners"));
                    Util.kv("  First owned since",      rs.getString("first_owned_since"));
                }
            }
        }
        System.out.println();
    }

    // ── 13. Register a New Agent ──────────────────────────────────────────────
    private void registerNewAgent() throws SQLException {
        Util.header("Register a New Agent");

        Util.prompt("First name: ");
        String firstName = sc.nextLine().trim();
        if (firstName.isEmpty()) { Util.error("First name cannot be empty."); return; }

        Util.prompt("Last name: ");
        String lastName = sc.nextLine().trim();
        if (lastName.isEmpty()) { Util.error("Last name cannot be empty."); return; }

        Util.prompt("Email: ");
        String email = sc.nextLine().trim();
        if (email.isEmpty()) { Util.error("Email cannot be empty."); return; }

        Util.prompt("Phone number: ");
        String phone = sc.nextLine().trim();
        if (phone.isEmpty()) { Util.error("Phone cannot be empty."); return; }

        double commission = Util.readPositiveDouble(sc, "Commission percent (e.g. 2.5): ");
        if (commission <= 0 || commission > 100) { Util.error("Commission must be between 0 and 100."); return; }

        int experience = Util.readPositiveInt(sc, "Years of experience: ");
        if (experience < 0) return;

        System.out.println();
        Util.subHeader("Confirm Agent Details");
        Util.kv("  Name",       firstName + " " + lastName);
        Util.kv("  Email",      email);
        Util.kv("  Phone",      phone);
        Util.kv("  Commission", commission + "%");
        Util.kv("  Experience", experience + " years");
        if (!Util.confirm(sc, "Register this agent?")) {
            Util.info("Cancelled."); return;
        }

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            // 1. Insert person
            int personId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(person_id),0)+1 FROM person");
                 ResultSet rs = ps.executeQuery()) {
                rs.next(); personId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO person(person_id, first_name, last_name, email) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, personId); ps.setString(2, firstName);
                ps.setString(3, lastName); ps.setString(4, email);
                ps.executeUpdate();
            }
            // 2. Insert phone
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO person_phone(person_id, phone_number) VALUES (?, ?)")) {
                ps.setInt(1, personId); ps.setString(2, phone);
                ps.executeUpdate();
            }
            // 3. Insert agent
            int agentId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(agent_id),0)+1 FROM agent");
                 ResultSet rs = ps.executeQuery()) {
                rs.next(); agentId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO agent(agent_id, person_id, commission_percent, experience_years) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, agentId); ps.setInt(2, personId);
                ps.setDouble(3, commission); ps.setInt(4, experience);
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println();
            Util.success("Agent registered successfully!");
            Util.kv("  Person ID", String.valueOf(personId));
            Util.kv("  Agent ID",  String.valueOf(agentId));
            System.out.println();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void listAgents() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, a.experience_years " +
            "FROM agent AS a JOIN person AS pe ON a.person_id = pe.person_id " +
            "ORDER BY a.agent_id";
        System.out.println();
        Util.info("Available agents:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("    [%2d]  %s %s  (%d yrs exp)%n",
                    rs.getInt("agent_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getInt("experience_years"));
            }
        }
        System.out.println();
    }

    private boolean agentExists(int agentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM agent WHERE agent_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void printAgentName(int agentId) throws SQLException {
        String sql = "SELECT pe.first_name, pe.last_name, a.commission_percent " +
                     "FROM agent AS a JOIN person AS pe ON a.person_id = pe.person_id " +
                     "WHERE a.agent_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Util.kv("Agent", rs.getString("first_name") + " " + rs.getString("last_name"));
                    Util.kv("Commission", rs.getString("commission_percent") + "%");
                } else {
                    Util.error("Agent ID " + agentId + " not found.");
                }
            }
        }
    }

    private void printAgentSaleTotals(int agentId) throws SQLException {
        String sql =
            "SELECT COUNT(*) AS cnt, COALESCE(SUM(t.final_amount),0) AS total " +
            "FROM listing AS l JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "WHERE l.agent_id = ? AND l.type = 'sale'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Util.separator();
                    Util.kv("  Total properties sold", rs.getString("cnt"));
                    Util.kv("  Total revenue",         Util.formatCurrency(rs.getString("total")));
                }
            }
        }
        System.out.println();
    }

    private void printAgentRentalTotals(int agentId) throws SQLException {
        String sql =
            "SELECT COUNT(*) AS cnt, COALESCE(SUM(rd.monthly_rent),0) AS total " +
            "FROM listing AS l " +
            "JOIN transaction_ AS t  ON l.listing_id    = t.listing_id " +
            "JOIN rent_detail  AS rd ON rd.transaction_id = t.transaction_id " +
            "WHERE l.agent_id = ? AND l.type = 'rent'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Util.separator();
                    Util.kv("  Total properties rented", rs.getString("cnt"));
                    Util.kv("  Total monthly rent",      Util.formatCurrency(rs.getString("total")));
                }
            }
        }
        System.out.println();
    }

    private void showAvailableLocalities() throws SQLException {
        String sql =
            "SELECT DISTINCT area, COUNT(*) AS properties " +
            "FROM property GROUP BY area ORDER BY area";
        Util.info("Available localities:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("    . %-20s (%d properties)%n",
                    rs.getString("area"), rs.getInt("properties"));
            }
        }
        System.out.println();
    }
}
