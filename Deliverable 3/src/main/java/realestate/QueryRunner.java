package realestate;

import java.sql.*;

/**
 * Executes the CS241 required queries (a-f) and additional admin queries.
 * Each method prints a formatted table to stdout.
 */
public class QueryRunner {

    // ─── REQUIRED QUERIES ────────────────────────────────────────────────────

    /**
     * (a) Houses built after 2023 that are available for rent.
     */
    public static void queryA() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.city, p.area, p.bedrooms, " +
            "       p.year_built, l.listing_id, l.listed_price AS monthly_rent, l.status " +
            "FROM property AS p " +
            "JOIN listing AS l ON p.property_id = l.property_id " +
            "WHERE p.property_type = 'house' " +
            "  AND p.year_built > 2023 " +
            "  AND l.type = 'rent' " +
            "  AND l.status = 'active'";

        printHeader("(a) Houses built after 2023 available for rent");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Prop ID", "Address", "City", "Area", "Beds",
                             "Year", "List ID", "Monthly Rent", "Status"};
            int[]    widths = {7, 22, 10, 12, 4, 6, 7, 14, 10};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("property_id"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getString("area"),
                    rs.getString("bedrooms"),
                    rs.getString("year_built"),
                    rs.getString("listing_id"),
                    "Rs. " + rs.getString("monthly_rent"),
                    rs.getString("status")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * (b) Average selling price of properties sold in 2018.
     */
    public static void queryB() throws SQLException {
        String sql =
            "SELECT ROUND(AVG(t.final_amount), 2) AS average_selling_price_2018 " +
            "FROM transaction_ AS t " +
            "JOIN listing AS l ON t.listing_id = l.listing_id " +
            "WHERE l.type = 'sale' " +
            "  AND YEAR(t.transaction_date) = 2018";

        printHeader("(b) Average selling price of properties sold in 2018");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                System.out.printf("  Average Selling Price (2018): Rs. %,.2f%n",
                        rs.getDouble("average_selling_price_2018"));
            } else {
                System.out.println("  No sale transactions found for 2018.");
            }
        }
        System.out.println();
    }

    /**
     * (c) Active rent listings on GS Road with >= 2 bedrooms and rent < Rs. 15,000.
     */
    public static void queryC() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.area, p.bedrooms, " +
            "       l.listing_id, l.listed_price AS monthly_rent " +
            "FROM property AS p " +
            "JOIN listing AS l ON p.property_id = l.property_id " +
            "WHERE p.area = 'GS Road' " +
            "  AND l.type = 'rent' " +
            "  AND l.status = 'active' " +
            "  AND p.bedrooms >= 2 " +
            "  AND l.listed_price < 15000";

        printHeader("(c) Active rent on GS Road, >=2 bedrooms, rent < Rs.15,000");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Prop ID", "Address", "Area", "Beds", "List ID", "Monthly Rent"};
            int[]    widths = {7, 22, 12, 5, 7, 14};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("property_id"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("bedrooms"),
                    rs.getString("listing_id"),
                    "Rs. " + rs.getString("monthly_rent")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * (d) Agent who sold the most properties in 2023
     *     (tie-broken by highest total sale amount).
     */
    public static void queryD() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       COUNT(*) AS properties_sold_2023, " +
            "       SUM(t.final_amount) AS total_sale_amount_2023 " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "JOIN listing AS l ON a.agent_id = l.agent_id " +
            "JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "WHERE l.type = 'sale' " +
            "  AND YEAR(t.transaction_date) = 2023 " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name " +
            "ORDER BY properties_sold_2023 DESC, total_sale_amount_2023 DESC " +
            "LIMIT 1";

        printHeader("(d) Top-selling agent in 2023");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                System.out.printf("  Agent ID   : %s%n", rs.getString("agent_id"));
                System.out.printf("  Name       : %s %s%n",
                        rs.getString("first_name"), rs.getString("last_name"));
                System.out.printf("  Properties : %s%n", rs.getString("properties_sold_2023"));
                System.out.printf("  Total Sales: Rs. %,.2f%n",
                        rs.getDouble("total_sale_amount_2023"));
            } else {
                System.out.println("  No sale transactions found for 2023.");
            }
        }
        System.out.println();
    }

    /**
     * (e) Per-agent average days on market for 2018 sale transactions.
     */
    public static void queryE() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       ROUND(AVG(DATEDIFF(t.transaction_date, l.list_date)), 2) " +
            "         AS avg_days_on_market " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "JOIN listing AS l ON a.agent_id = l.agent_id " +
            "JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "WHERE l.type = 'sale' " +
            "  AND YEAR(t.transaction_date) = 2018 " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name " +
            "ORDER BY avg_days_on_market DESC, a.agent_id";

        printHeader("(e) Avg days on market per agent (2018 sales)");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Agent ID", "First Name", "Last Name", "Avg Days on Market"};
            int[]    widths = {8, 14, 14, 20};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("agent_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("avg_days_on_market")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * (f) Most expensive house for sale AND house with highest rent.
     */
    public static void queryF() throws SQLException {
        String sql =
            "SELECT 'Most Expensive House (Sale)' AS query_type, " +
            "       p.property_id, p.address, p.city, p.area, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       l.listing_id, l.type AS listing_type, l.listed_price, l.status " +
            "FROM property AS p " +
            "JOIN listing AS l ON p.property_id = l.property_id " +
            "WHERE p.property_type = 'house' AND l.type = 'sale' " +
            "  AND l.listed_price = ( " +
            "      SELECT MAX(l2.listed_price) " +
            "      FROM property AS p2 " +
            "      JOIN listing AS l2 ON p2.property_id = l2.property_id " +
            "      WHERE p2.property_type = 'house' AND l2.type = 'sale') " +
            "UNION ALL " +
            "SELECT 'House With Highest Rent' AS query_type, " +
            "       p.property_id, p.address, p.city, p.area, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       l.listing_id, l.type AS listing_type, l.listed_price, l.status " +
            "FROM property AS p " +
            "JOIN listing AS l ON p.property_id = l.property_id " +
            "WHERE p.property_type = 'house' AND l.type = 'rent' " +
            "  AND l.listed_price = ( " +
            "      SELECT MAX(l2.listed_price) " +
            "      FROM property AS p2 " +
            "      JOIN listing AS l2 ON p2.property_id = l2.property_id " +
            "      WHERE p2.property_type = 'house' AND l2.type = 'rent')";

        printHeader("(f) Most expensive house for sale & house with highest rent");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Type", "Prop ID", "Address", "Area",
                             "Sqft", "Beds", "Year", "Price", "Status"};
            int[]    widths = {28, 7, 20, 12, 6, 5, 6, 14, 10};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("query_type"),
                    rs.getString("property_id"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("size_sqft"),
                    rs.getString("bedrooms"),
                    rs.getString("year_built"),
                    "Rs. " + rs.getString("listed_price"),
                    rs.getString("status")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    // ─── ADDITIONAL / BONUS QUERIES ──────────────────────────────────────────

    /**
     * Houses in Guwahati costing between Rs. 20,00,000 and Rs. 60,00,000.
     * (Project description query b — finding houses in a price range.)
     */
    public static void queryBonus1() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.city, p.area, " +
            "       p.bedrooms, p.size_sqft, l.listed_price " +
            "FROM property AS p " +
            "JOIN listing AS l ON p.property_id = l.property_id " +
            "WHERE p.city = 'Guwahati' " +
            "  AND p.property_type = 'house' " +
            "  AND l.type = 'sale' " +
            "  AND l.listed_price BETWEEN 2000000 AND 6000000 " +
            "ORDER BY l.listed_price";

        printHeader("(Bonus 1) Houses in Guwahati priced Rs.20L – Rs.60L");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Prop ID", "Address", "City", "Area", "Beds", "Sqft", "Listed Price"};
            int[]    widths = {7, 20, 10, 12, 5, 6, 14};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("property_id"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getString("area"),
                    rs.getString("bedrooms"),
                    rs.getString("size_sqft"),
                    "Rs. " + rs.getString("listed_price")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * All active listings — both sale and rent.
     */
    public static void queryBonus2() throws SQLException {
        String sql =
            "SELECT l.listing_id, p.address, p.area, p.property_type, " +
            "       l.type, l.listed_price, l.list_date, " +
            "       pe.first_name, pe.last_name " +
            "FROM listing AS l " +
            "JOIN property AS p ON l.property_id = p.property_id " +
            "JOIN agent    AS a ON l.agent_id    = a.agent_id " +
            "JOIN person   AS pe ON a.person_id  = pe.person_id " +
            "WHERE l.status = 'active' " +
            "ORDER BY l.type, l.listed_price DESC";

        printHeader("(Bonus 2) All currently active listings");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"List ID", "Address", "Area", "Type", "For",
                             "Price", "Listed On", "Agent"};
            int[]    widths = {7, 20, 12, 10, 5, 14, 12, 20};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("listing_id"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("property_type"),
                    rs.getString("type"),
                    "Rs. " + rs.getString("listed_price"),
                    rs.getString("list_date"),
                    rs.getString("first_name") + " " + rs.getString("last_name")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * Complete transaction history with property and customer details.
     */
    public static void queryBonus3() throws SQLException {
        String sql =
            "SELECT t.transaction_id, t.transaction_date, " +
            "       p.address, p.area, l.type AS txn_type, " +
            "       t.final_amount, " +
            "       pe.first_name, pe.last_name, " +
            "       ag.first_name AS agent_fn, ag.last_name AS agent_ln " +
            "FROM transaction_ AS t " +
            "JOIN listing  AS l  ON t.listing_id  = l.listing_id " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN person   AS pe ON t.customer_id = pe.person_id " +
            "JOIN agent    AS a  ON l.agent_id    = a.agent_id " +
            "JOIN person   AS ag ON a.person_id   = ag.person_id " +
            "ORDER BY t.transaction_date";

        printHeader("(Bonus 3) Full transaction history");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Txn ID", "Date", "Address", "Area", "Type",
                             "Amount", "Customer", "Agent"};
            int[]    widths = {6, 12, 20, 12, 5, 14, 18, 18};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("transaction_id"),
                    rs.getString("transaction_date"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("txn_type"),
                    "Rs. " + rs.getString("final_amount"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("agent_fn") + " " + rs.getString("agent_ln")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * All agents with their total sales count and total revenue.
     */
    public static void queryBonus4() throws SQLException {
        String sql =
            "SELECT a.agent_id, pe.first_name, pe.last_name, " +
            "       a.commission_percent, a.experience_years, " +
            "       COUNT(t.transaction_id) AS total_transactions, " +
            "       COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END), 0) " +
            "         AS total_sales_revenue " +
            "FROM agent AS a " +
            "JOIN person AS pe ON a.person_id = pe.person_id " +
            "LEFT JOIN listing AS l ON a.agent_id = l.agent_id " +
            "LEFT JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "GROUP BY a.agent_id, pe.first_name, pe.last_name, " +
            "         a.commission_percent, a.experience_years " +
            "ORDER BY total_sales_revenue DESC";

        printHeader("(Bonus 4) Agent performance summary");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"ID", "First", "Last", "Comm%", "Exp(yr)",
                             "Txns", "Total Sale Revenue"};
            int[]    widths = {4, 12, 12, 6, 7, 5, 20};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("agent_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("commission_percent") + "%",
                    rs.getString("experience_years"),
                    rs.getString("total_transactions"),
                    "Rs. " + rs.getString("total_sales_revenue")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * Current ownership — who owns what right now.
     */
    public static void queryBonus5() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.area, p.property_type, " +
            "       p.year_built, pe.first_name, pe.last_name, " +
            "       pe.email, o.start_date " +
            "FROM ownership AS o " +
            "JOIN property AS p ON o.property_id = p.property_id " +
            "JOIN person   AS pe ON o.owner_id   = pe.person_id " +
            "WHERE o.end_date IS NULL " +
            "ORDER BY p.property_id";

        printHeader("(Bonus 5) Current property ownership");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"Prop ID", "Address", "Area", "Type", "Built",
                             "Owner", "Email", "Since"};
            int[]    widths = {7, 20, 12, 10, 6, 18, 28, 12};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("property_id"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("property_type"),
                    rs.getString("year_built"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("start_date")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    /**
     * Rent details — active leases with tenant and property info.
     */
    public static void queryBonus6() throws SQLException {
        String sql =
            "SELECT rd.rent_detail_id, p.address, p.area, " +
            "       rd.lease_start, rd.lease_end, rd.monthly_rent, " +
            "       pe.first_name, pe.last_name " +
            "FROM rent_detail AS rd " +
            "JOIN transaction_ AS t ON rd.transaction_id = t.transaction_id " +
            "JOIN listing  AS l  ON t.listing_id  = l.listing_id " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN person   AS pe ON t.customer_id = pe.person_id " +
            "ORDER BY rd.lease_start";

        printHeader("(Bonus 6) Rent detail — active leases");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String[] cols = {"RD ID", "Address", "Area",
                             "Lease Start", "Lease End", "Monthly Rent", "Tenant"};
            int[]    widths = {5, 20, 12, 12, 12, 14, 20};
            printRow(cols, widths);
            printDivider(widths);

            int count = 0;
            while (rs.next()) {
                printRow(new String[]{
                    rs.getString("rent_detail_id"),
                    rs.getString("address"),
                    rs.getString("area"),
                    rs.getString("lease_start"),
                    rs.getString("lease_end"),
                    "Rs. " + rs.getString("monthly_rent"),
                    rs.getString("first_name") + " " + rs.getString("last_name")
                }, widths);
                count++;
            }
            printFooter(count);
        }
    }

    // ─── TABLE FORMATTING UTILITIES ──────────────────────────────────────────

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  %-60s║%n", title);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    static void printRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < values.length; i++) {
            String val = (values[i] == null) ? "NULL" : values[i];
            if (val.length() > widths[i]) {
                val = val.substring(0, widths[i] - 1) + "…";
            }
            sb.append(String.format("%-" + widths[i] + "s", val));
            if (i < values.length - 1) sb.append("  ");
        }
        System.out.println(sb);
    }

    static void printDivider(int[] widths) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < widths.length; i++) {
            sb.append("-".repeat(widths[i]));
            if (i < widths.length - 1) sb.append("  ");
        }
        System.out.println(sb);
    }

    private static void printFooter(int count) {
        System.out.println();
        System.out.printf("  [%d row(s) returned]%n", count);
        System.out.println();
    }

    // ─── CUSTOM QUERY ────────────────────────────────────────────────────────

    /**
     * Executes any SQL string typed by the user.
     * SELECT statements print a formatted table.
     * INSERT / UPDATE / DELETE print rows affected.
     */
    public static void runCustomQuery(String sql) throws SQLException {
        printHeader("Custom Query");
        System.out.println("  SQL: " + sql);
        System.out.println();

        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement()) {

            boolean isResultSet = st.execute(sql);

            if (isResultSet) {
                // SELECT — print formatted table
                try (ResultSet rs = st.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    // Build column names and auto-size widths
                    String[] colNames = new String[colCount];
                    int[]    widths   = new int[colCount];
                    for (int i = 0; i < colCount; i++) {
                        colNames[i] = meta.getColumnLabel(i + 1);
                        widths[i]   = Math.max(colNames[i].length(), 12);
                    }

                    // First pass — widen columns based on data
                    java.util.List<String[]> rows = new java.util.ArrayList<>();
                    while (rs.next()) {
                        String[] row = new String[colCount];
                        for (int i = 0; i < colCount; i++) {
                            String val = rs.getString(i + 1);
                            row[i]   = (val == null) ? "NULL" : val;
                            widths[i] = Math.min(30, Math.max(widths[i], row[i].length()));
                        }
                        rows.add(row);
                    }

                    // Print header row and all data rows
                    printRow(colNames, widths);
                    printDivider(widths);
                    for (String[] row : rows) printRow(row, widths);
                    printFooter(rows.size());
                }
            } else {
                // INSERT / UPDATE / DELETE
                int affected = st.getUpdateCount();
                System.out.printf("  [OK] Query executed. %d row(s) affected.%n%n", affected);
            }
        }
    }

}