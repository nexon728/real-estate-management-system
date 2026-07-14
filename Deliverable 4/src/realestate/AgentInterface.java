package realestate;

import java.sql.*;
import java.util.Scanner;

/**
 * CS241 Deliverable 4 — Interface for Agents.
 *
 * Agents can:
 *   1. View their own active listings
 *   2. Record a SALE   (mark listing sold, record transaction, update ownership)
 *   3. Record a RENTAL (mark listing rented, record transaction + rent_detail)
 *   4. Add a new listing for an existing property
 *   5. View their own full transaction history
 *   6. View all properties in the system
 *   7. View their performance summary
 */
public class AgentInterface {

    private final Scanner sc;

    public AgentInterface(Scanner sc) { this.sc = sc; }

    // ── LOGIN + MENU ──────────────────────────────────────────────────────────
    public void run() {
        int agentId = login();
        if (agentId == -1) return;

        boolean back = false;
        while (!back) {
            printMenu(agentId);
            Util.prompt("Enter choice: ");
            String ch = sc.nextLine().trim();
            System.out.println();
            try {
                switch (ch) {
                    case "1": viewMyListings(agentId);          break;
                    case "2": recordSale(agentId);              break;
                    case "3": recordRental(agentId);            break;
                    case "4": addNewListing(agentId);           break;
                    case "5": viewMyTransactionHistory(agentId);break;
                    case "6": viewAllProperties();              break;
                    case "7": viewMyPerformance(agentId);       break;
                    case "8": withdrawListing(agentId);         break;
                    case "9": updateListingPrice(agentId);      break;
                    case "10": registerNewPerson();             break;
                    case "11": registerNewProperty();           break;
                    case "0": back = true;
                              Util.success("Logged out successfully.");
                              break;
                    default:  Util.error("Invalid choice. Enter 0-11."); break;
                }
            } catch (SQLException e) {
                Util.error("SQL Error: " + e.getMessage());
                System.out.println("  " + Util.DIM + "SQLState: " + e.getSQLState()
                    + " | Code: " + e.getErrorCode() + Util.RESET);
            }
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    private int login() {
        Util.header("Agent Login");
        int id = Util.readPositiveInt(sc, "Enter your Agent ID: ");
        if (id <= 0) { Util.error("Invalid Agent ID."); return -1; }

        try {
            String sql =
                "SELECT pe.first_name, pe.last_name, a.experience_years, a.commission_percent " +
                "FROM agent AS a JOIN person AS pe ON a.person_id = pe.person_id " +
                "WHERE a.agent_id = ?";
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println();
                        Util.success("Welcome, " + rs.getString("first_name")
                            + " " + rs.getString("last_name") + "!");
                        Util.kv("Agent ID",    String.valueOf(id));
                        Util.kv("Experience",  rs.getString("experience_years") + " years");
                        Util.kv("Commission",  rs.getString("commission_percent") + "%");
                        return id;
                    } else {
                        Util.error("Agent ID " + id + " not found in the system.");
                        return -1;
                    }
                }
            }
        } catch (SQLException e) {
            Util.error("Database error during login: " + e.getMessage());
            return -1;
        }
    }

    private void printMenu(int agentId) {
        System.out.println();
        System.out.println(Util.YELLOW
            + "  ┌──────────────────────────────────────────────────────────────┐"
            + "\n  │          AGENT PORTAL  (Agent ID: " + String.format("%-28s", agentId + ")")  + "│"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  1  View my active listings                                 │"
            + "\n  │  2  Record a sale transaction                               │"
            + "\n  │  3  Record a rental transaction                             │"
            + "\n  │  4  Add a new listing for a property                        │"
            + "\n  │  5  View my full transaction history                        │"
            + "\n  │  6  View all properties                                     │"
            + "\n  │  7  My performance summary                                  │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  8  Withdraw a listing                                      │"
            + "\n  │  9  Update listing price                                    │"
            + "\n  │  10 Register a new person (buyer/tenant)                    │"
            + "\n  │  11 Register a new property                                 │"
            + "\n  ├──────────────────────────────────────────────────────────────┤"
            + "\n  │  0  Logout / Back to main menu                              │"
            + "\n  └──────────────────────────────────────────────────────────────┘"
            + Util.RESET);
    }

    // ── 1. VIEW MY ACTIVE LISTINGS ────────────────────────────────────────────
    private void viewMyListings(int agentId) throws SQLException {
        String sql =
            "SELECT l.listing_id, l.type, l.listed_price, l.list_date, l.status, " +
            "       p.property_id, p.address, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built " +
            "FROM listing  AS l " +
            "JOIN property AS p ON l.property_id = p.property_id " +
            "WHERE l.agent_id = ? AND l.status = 'active' " +
            "ORDER BY l.type, l.list_date";

        Util.header("My Active Listings");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }
    }

    // ── 2. RECORD A SALE ─────────────────────────────────────────────────────
    private void recordSale(int agentId) throws SQLException {
        Util.header("Record a Sale Transaction");

        // Show active SALE listings for this agent
        showActiveSaleListings(agentId);
        if (!hasActiveListings(agentId, "sale")) {
            Util.info("You have no active sale listings to process.");
            return;
        }

        // Get listing ID
        int listingId = Util.readIntOrCancel(sc, "Enter Listing ID to mark as SOLD (or 0 to cancel): ");
        if (listingId <= 0) return;
        if (!validateListingBelongsToAgent(listingId, agentId, "sale")) {
            Util.error("Listing not found, not yours, or not an active sale listing.");
            return;
        }

        // Get buyer (customer) ID
        showAllPersons();
        int buyerId = Util.readIntOrCancel(sc, "Enter Buyer's Person ID (or 0 to cancel): ");
        if (buyerId <= 0) return;
        if (!personExists(buyerId)) { Util.error("Person ID not found."); return; }

        // Get transaction details
        String txnDate = Util.readDate(sc, "Enter sale date (YYYY-MM-DD): ");
        if (txnDate == null) return;

        double amount = Util.readPositiveDouble(sc, "Enter final selling price (Rs.): ");
        if (amount <= 0) return;

        // Confirmation
        System.out.println();
        Util.subHeader("Confirm Sale Details");
        Util.kv("  Listing ID", String.valueOf(listingId));
        Util.kv("  Buyer ID",   String.valueOf(buyerId));
        Util.kv("  Sale Date",  txnDate);
        Util.kv("  Amount",     Util.formatCurrency(amount));
        if (!Util.confirm(sc, "Proceed with this sale?")) {
            Util.info("Sale cancelled.");
            return;
        }

        // New transaction ID
        int txnId = nextId("SELECT COALESCE(MAX(transaction_id),0)+1 FROM transaction_");

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            String ins =
                "INSERT INTO transaction_(transaction_id, listing_id, customer_id, " +
                "  transaction_date, final_amount) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                ps.setInt(1, txnId);
                ps.setInt(2, listingId);
                ps.setInt(3, buyerId);
                ps.setString(4, txnDate);
                ps.setDouble(5, amount);
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println();
            Util.success("Sale recorded! Transaction ID: " + txnId);
            Util.info("Listing " + listingId + " is now marked as SOLD.");
            Util.info("Ownership transferred to buyer (Person ID: " + buyerId + ").");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── 3. RECORD A RENTAL ────────────────────────────────────────────────────
    private void recordRental(int agentId) throws SQLException {
        Util.header("Record a Rental Transaction");

        showActiveRentListings(agentId);
        if (!hasActiveListings(agentId, "rent")) {
            Util.info("You have no active rent listings to process.");
            return;
        }

        int listingId = Util.readIntOrCancel(sc, "Enter Listing ID to mark as RENTED (or 0 to cancel): ");
        if (listingId <= 0) return;
        if (!validateListingBelongsToAgent(listingId, agentId, "rent")) {
            Util.error("Listing not found, not yours, or not an active rent listing.");
            return;
        }

        showAllPersons();
        int tenantId = Util.readIntOrCancel(sc, "Enter Tenant's Person ID (or 0 to cancel): ");
        if (tenantId <= 0) return;
        if (!personExists(tenantId)) { Util.error("Person ID not found."); return; }

        String txnDate = Util.readDate(sc, "Enter transaction date (YYYY-MM-DD): ");
        if (txnDate == null) return;

        double monthlyRent = Util.readPositiveDouble(sc, "Enter agreed monthly rent (Rs.): ");
        if (monthlyRent <= 0) return;

        String leaseStart = Util.readDate(sc, "Enter lease start date (YYYY-MM-DD): ");
        if (leaseStart == null) return;

        String leaseEnd = Util.readDate(sc, "Enter lease end date   (YYYY-MM-DD): ");
        if (leaseEnd == null) return;

        if (leaseEnd.compareTo(leaseStart) <= 0) {
            Util.error("Lease end date must be after lease start date.");
            return;
        }

        // Confirmation
        System.out.println();
        Util.subHeader("Confirm Rental Details");
        Util.kv("  Listing ID",   String.valueOf(listingId));
        Util.kv("  Tenant ID",    String.valueOf(tenantId));
        Util.kv("  Monthly Rent", Util.formatCurrency(monthlyRent));
        Util.kv("  Lease Period", leaseStart + " to " + leaseEnd);
        if (!Util.confirm(sc, "Proceed with this rental?")) {
            Util.info("Rental cancelled.");
            return;
        }

        int txnId = nextId("SELECT COALESCE(MAX(transaction_id),0)+1 FROM transaction_");
        int rdId  = nextId("SELECT COALESCE(MAX(rent_detail_id),0)+1 FROM rent_detail");

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            // 1. Insert transaction
            String insTxn =
                "INSERT INTO transaction_(transaction_id, listing_id, customer_id, " +
                "  transaction_date, final_amount) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insTxn)) {
                ps.setInt(1, txnId);
                ps.setInt(2, listingId);
                ps.setInt(3, tenantId);
                ps.setString(4, txnDate);
                ps.setDouble(5, monthlyRent);
                ps.executeUpdate();
            }
            // 2. Insert rent_detail
            String insRd =
                "INSERT INTO rent_detail(rent_detail_id, transaction_id, " +
                "  lease_start, lease_end, monthly_rent) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insRd)) {
                ps.setInt(1, rdId);
                ps.setInt(2, txnId);
                ps.setString(3, leaseStart);
                ps.setString(4, leaseEnd);
                ps.setDouble(5, monthlyRent);
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println();
            Util.success("Rental recorded! Transaction ID: " + txnId + "  Rent Detail ID: " + rdId);
            Util.info("Listing " + listingId + " is now marked as RENTED.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── 4. ADD A NEW LISTING ──────────────────────────────────────────────────
    private void addNewListing(int agentId) throws SQLException {
        Util.header("Add a New Listing");

        showListableProperties();
        int propId = Util.readIntOrCancel(sc, "Enter Property ID to list (or 0 to cancel): ");
        if (propId <= 0) return;
        if (!propertyExists(propId)) { Util.error("Property ID not found."); return; }

        // Check if property already has an active listing
        if (hasActiveListing(propId)) {
            Util.error("Property " + propId + " already has an active listing.");
            return;
        }

        Util.prompt("Listing type — enter 'sale' or 'rent': ");
        String type = sc.nextLine().trim().toLowerCase();
        if (!type.equals("sale") && !type.equals("rent")) {
            Util.error("Type must be 'sale' or 'rent'."); return;
        }

        double price = Util.readPositiveDouble(sc, "Enter listed price (Rs.): ");
        if (price <= 0) return;

        String listDate = Util.readDate(sc, "Enter listing date (YYYY-MM-DD): ");
        if (listDate == null) return;

        // Confirmation
        System.out.println();
        Util.subHeader("Confirm Listing Details");
        Util.kv("  Property ID", String.valueOf(propId));
        Util.kv("  Type",        type);
        Util.kv("  Price",       Util.formatCurrency(price));
        Util.kv("  Date",        listDate);
        if (!Util.confirm(sc, "Create this listing?")) {
            Util.info("Listing creation cancelled.");
            return;
        }

        int listingId = nextId("SELECT COALESCE(MAX(listing_id),0)+1 FROM listing");

        String ins =
            "INSERT INTO listing(listing_id, property_id, agent_id, type, " +
            "  listed_price, list_date, status) VALUES (?, ?, ?, ?, ?, ?, 'active')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(ins)) {
            ps.setInt(1, listingId);
            ps.setInt(2, propId);
            ps.setInt(3, agentId);
            ps.setString(4, type);
            ps.setDouble(5, price);
            ps.setString(6, listDate);
            ps.executeUpdate();
        }
        System.out.println();
        Util.success("New listing created! Listing ID: " + listingId);
        Util.kv("  Property", String.valueOf(propId));
        Util.kv("  Type",     type);
        Util.kv("  Price",    Util.formatCurrency(price));
        System.out.println();
    }

    // ── 5. MY TRANSACTION HISTORY ─────────────────────────────────────────────
    private void viewMyTransactionHistory(int agentId) throws SQLException {
        String sql =
            "SELECT t.transaction_id, t.transaction_date, l.type AS txn_type, " +
            "       p.address, p.area, p.property_type, " +
            "       t.final_amount, " +
            "       pe.first_name AS customer_fn, pe.last_name AS customer_ln " +
            "FROM transaction_ AS t " +
            "JOIN listing  AS l  ON t.listing_id  = l.listing_id " +
            "JOIN property AS p  ON l.property_id = p.property_id " +
            "JOIN person   AS pe ON t.customer_id = pe.person_id " +
            "WHERE l.agent_id = ? " +
            "ORDER BY t.transaction_date";

        Util.header("My Transaction History");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                Util.printResultSet(rs);
            }
        }

        // Summary totals
        String sumSql =
            "SELECT " +
            "  COUNT(CASE WHEN l.type='sale' THEN 1 END) AS sales_count, " +
            "  COUNT(CASE WHEN l.type='rent' THEN 1 END) AS rent_count, " +
            "  COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END),0) AS sales_revenue " +
            "FROM transaction_ AS t " +
            "JOIN listing AS l ON t.listing_id = l.listing_id " +
            "WHERE l.agent_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sumSql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Util.separator();
                    Util.kv("  Total sales",        rs.getString("sales_count"));
                    Util.kv("  Total rentals",      rs.getString("rent_count"));
                    Util.kv("  Total sales revenue", Util.formatCurrency(rs.getString("sales_revenue")));
                }
            }
        }
        System.out.println();
    }

    // ── 6. VIEW ALL PROPERTIES ────────────────────────────────────────────────
    private void viewAllProperties() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built, " +
            "       COALESCE(l.status, 'No listing') AS latest_status " +
            "FROM property AS p " +
            "LEFT JOIN listing AS l ON p.property_id = l.property_id " +
            "  AND l.listing_id = (SELECT MAX(l2.listing_id) FROM listing l2 " +
            "                      WHERE l2.property_id = p.property_id) " +
            "ORDER BY p.property_id";

        Util.header("All Properties");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── 7. MY PERFORMANCE SUMMARY ─────────────────────────────────────────────
    private void viewMyPerformance(int agentId) throws SQLException {
        Util.header("My Performance Summary");

        String sql =
            "SELECT " +
            "  COUNT(DISTINCT CASE WHEN l.type='sale' AND t.transaction_id IS NOT NULL THEN t.transaction_id END) AS total_sales, " +
            "  COUNT(DISTINCT CASE WHEN l.type='rent' AND t.transaction_id IS NOT NULL THEN t.transaction_id END) AS total_rentals, " +
            "  COUNT(DISTINCT CASE WHEN l.status='active' THEN l.listing_id END) AS active_listings, " +
            "  COALESCE(SUM(CASE WHEN l.type='sale' THEN t.final_amount END), 0) AS sale_revenue, " +
            "  COALESCE(SUM(CASE WHEN l.type='rent' THEN t.final_amount END), 0) AS rent_revenue " +
            "FROM listing AS l " +
            "LEFT JOIN transaction_ AS t ON l.listing_id = t.listing_id " +
            "WHERE l.agent_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Util.kv("Total Sales",       rs.getString("total_sales"));
                    Util.kv("Total Rentals",     rs.getString("total_rentals"));
                    Util.kv("Active Listings",   rs.getString("active_listings"));
                    Util.kv("Sale Revenue",      Util.formatCurrency(rs.getString("sale_revenue")));
                    Util.kv("Rent Revenue",      Util.formatCurrency(rs.getString("rent_revenue")));
                }
            }
        }
        System.out.println();
    }

    // ── 8. WITHDRAW A LISTING ─────────────────────────────────────────────────
    private void withdrawListing(int agentId) throws SQLException {
        Util.header("Withdraw a Listing");
        // Show agent's active listings
        String showSql =
            "SELECT l.listing_id, l.type, p.address, p.area, l.listed_price, l.list_date " +
            "FROM listing l JOIN property p ON l.property_id = p.property_id " +
            "WHERE l.agent_id = ? AND l.status = 'active' ORDER BY l.listing_id";
        Util.info("Your active listings:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(showSql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
        }

        int listingId = Util.readIntOrCancel(sc, "Enter Listing ID to withdraw (or 0 to cancel): ");
        if (listingId <= 0) return;

        // Validate
        String valSql = "SELECT COUNT(*) FROM listing WHERE listing_id=? AND agent_id=? AND status='active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(valSql)) {
            ps.setInt(1, listingId); ps.setInt(2, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    Util.error("Listing not found, not yours, or not active.");
                    return;
                }
            }
        }

        if (!Util.confirm(sc, "Withdraw listing " + listingId + "?")) {
            Util.info("Cancelled."); return;
        }

        String upd = "UPDATE listing SET status = 'withdrawn' WHERE listing_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(upd)) {
            ps.setInt(1, listingId);
            ps.executeUpdate();
        }
        Util.success("Listing " + listingId + " has been withdrawn.");
    }

    // ── 9. UPDATE LISTING PRICE ───────────────────────────────────────────────
    private void updateListingPrice(int agentId) throws SQLException {
        Util.header("Update Listing Price");
        String showSql =
            "SELECT l.listing_id, l.type, p.address, p.area, l.listed_price, l.list_date " +
            "FROM listing l JOIN property p ON l.property_id = p.property_id " +
            "WHERE l.agent_id = ? AND l.status = 'active' ORDER BY l.listing_id";
        Util.info("Your active listings:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(showSql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
        }

        int listingId = Util.readIntOrCancel(sc, "Enter Listing ID to update price (or 0 to cancel): ");
        if (listingId <= 0) return;

        String valSql = "SELECT listed_price FROM listing WHERE listing_id=? AND agent_id=? AND status='active'";
        double oldPrice;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(valSql)) {
            ps.setInt(1, listingId); ps.setInt(2, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Util.error("Listing not found, not yours, or not active.");
                    return;
                }
                oldPrice = rs.getDouble("listed_price");
            }
        }

        Util.kv("  Current Price", Util.formatCurrency(oldPrice));
        double newPrice = Util.readPositiveDouble(sc, "Enter new price (Rs.): ");
        if (newPrice <= 0) return;

        if (!Util.confirm(sc, "Update price from " + Util.formatCurrency(oldPrice) + " to " + Util.formatCurrency(newPrice) + "?")) {
            Util.info("Cancelled."); return;
        }

        String upd = "UPDATE listing SET listed_price = ? WHERE listing_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(upd)) {
            ps.setDouble(1, newPrice); ps.setInt(2, listingId);
            ps.executeUpdate();
        }
        Util.success("Listing " + listingId + " price updated to " + Util.formatCurrency(newPrice));
    }

    // ── 10. REGISTER A NEW PERSON ─────────────────────────────────────────────
    private void registerNewPerson() throws SQLException {
        Util.header("Register a New Person (Buyer / Tenant)");

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

        System.out.println();
        Util.subHeader("Confirm Person Details");
        Util.kv("  Name",  firstName + " " + lastName);
        Util.kv("  Email", email);
        Util.kv("  Phone", phone);
        if (!Util.confirm(sc, "Register this person?")) {
            Util.info("Cancelled."); return;
        }

        int personId = nextId("SELECT COALESCE(MAX(person_id),0)+1 FROM person");

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            String insPerson = "INSERT INTO person(person_id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insPerson)) {
                ps.setInt(1, personId); ps.setString(2, firstName);
                ps.setString(3, lastName); ps.setString(4, email);
                ps.executeUpdate();
            }
            String insPhone = "INSERT INTO person_phone(person_id, phone_number) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insPhone)) {
                ps.setInt(1, personId); ps.setString(2, phone);
                ps.executeUpdate();
            }
            conn.commit();
            Util.success("Person registered! Person ID: " + personId);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── 11. REGISTER A NEW PROPERTY ───────────────────────────────────────────
    private void registerNewProperty() throws SQLException {
        Util.header("Register a New Property");

        Util.prompt("Address: ");
        String address = sc.nextLine().trim();
        if (address.isEmpty()) { Util.error("Address cannot be empty."); return; }

        Util.prompt("City (e.g. Guwahati): ");
        String city = sc.nextLine().trim();
        if (city.isEmpty()) { Util.error("City cannot be empty."); return; }

        Util.prompt("Area/Locality (e.g. GS Road, Beltola): ");
        String area = sc.nextLine().trim();
        if (area.isEmpty()) { Util.error("Area cannot be empty."); return; }

        Util.prompt("Property type — enter 'house' or 'apartment': ");
        String type = sc.nextLine().trim().toLowerCase();
        if (!type.equals("house") && !type.equals("apartment")) {
            Util.error("Type must be 'house' or 'apartment'."); return;
        }

        int sizeSqft = Util.readPositiveInt(sc, "Size in sq.ft: ");
        if (sizeSqft <= 0) return;

        int bedrooms = Util.readPositiveInt(sc, "Number of bedrooms: ");
        if (bedrooms <= 0) return;

        int yearBuilt = Util.readPositiveInt(sc, "Year built: ");
        if (yearBuilt < 1900 || yearBuilt > 2100) {
            Util.error("Year must be between 1900 and 2100."); return;
        }

        // Owner
        showAllPersons();
        int ownerId = Util.readIntOrCancel(sc, "Enter Owner's Person ID (or 0 to cancel): ");
        if (ownerId <= 0) return;
        if (!personExists(ownerId)) { Util.error("Person ID not found."); return; }

        String ownerDate = Util.readDate(sc, "Enter ownership start date (YYYY-MM-DD): ");
        if (ownerDate == null) return;

        System.out.println();
        Util.subHeader("Confirm Property Details");
        Util.kv("  Address",  address + ", " + area + ", " + city);
        Util.kv("  Type",     type);
        Util.kv("  Size",     sizeSqft + " sq.ft, " + bedrooms + " bed(s)");
        Util.kv("  Year",     String.valueOf(yearBuilt));
        Util.kv("  Owner ID", String.valueOf(ownerId));
        if (!Util.confirm(sc, "Register this property?")) {
            Util.info("Cancelled."); return;
        }

        int propId = nextId("SELECT COALESCE(MAX(property_id),0)+1 FROM property");

        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            String insProp = "INSERT INTO property(property_id, address, city, area, property_type, size_sqft, bedrooms, year_built) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insProp)) {
                ps.setInt(1, propId); ps.setString(2, address); ps.setString(3, city);
                ps.setString(4, area); ps.setString(5, type); ps.setInt(6, sizeSqft);
                ps.setInt(7, bedrooms); ps.setInt(8, yearBuilt);
                ps.executeUpdate();
            }
            String insOwn = "INSERT INTO ownership(property_id, owner_id, start_date, end_date) VALUES (?, ?, ?, NULL)";
            try (PreparedStatement ps = conn.prepareStatement(insOwn)) {
                ps.setInt(1, propId); ps.setInt(2, ownerId); ps.setString(3, ownerDate);
                ps.executeUpdate();
            }
            conn.commit();
            Util.success("Property registered! Property ID: " + propId);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Helper display methods ────────────────────────────────────────────────
    private void showActiveSaleListings(int agentId) throws SQLException {
        String sql =
            "SELECT l.listing_id, p.address, p.area, p.property_type, " +
            "       p.bedrooms, l.listed_price, l.list_date " +
            "FROM listing AS l JOIN property AS p ON l.property_id = p.property_id " +
            "WHERE l.agent_id = ? AND l.type = 'sale' AND l.status = 'active'";
        Util.info("Your active SALE listings:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
        }
    }

    private void showActiveRentListings(int agentId) throws SQLException {
        String sql =
            "SELECT l.listing_id, p.address, p.area, p.property_type, " +
            "       p.bedrooms, l.listed_price, l.list_date " +
            "FROM listing AS l JOIN property AS p ON l.property_id = p.property_id " +
            "WHERE l.agent_id = ? AND l.type = 'rent' AND l.status = 'active'";
        Util.info("Your active RENT listings:");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) { Util.printResultSet(rs); }
        }
    }

    private void showListableProperties() throws SQLException {
        String sql =
            "SELECT p.property_id, p.address, p.area, p.property_type, " +
            "       p.size_sqft, p.bedrooms, p.year_built " +
            "FROM property AS p " +
            "WHERE p.property_id NOT IN ( " +
            "    SELECT property_id FROM listing WHERE status = 'active' " +
            ") " +
            "ORDER BY p.property_id";
        Util.info("Properties available to list (no current active listing):");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    private void showAllPersons() throws SQLException {
        String sql =
            "SELECT p.person_id, p.first_name, p.last_name, p.email " +
            "FROM person AS p " +
            "WHERE p.person_id NOT IN (SELECT person_id FROM agent) " +
            "ORDER BY p.person_id";
        Util.info("Registered persons (non-agents):");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Util.printResultSet(rs);
        }
    }

    // ── Validation helpers ────────────────────────────────────────────────────
    private boolean hasActiveListings(int agentId, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM listing WHERE agent_id=? AND type=? AND status='active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean hasActiveListing(int propId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM listing WHERE property_id=? AND status='active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, propId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean validateListingBelongsToAgent(int listingId, int agentId, String type)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM listing " +
                     "WHERE listing_id=? AND agent_id=? AND type=? AND status='active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, listingId); ps.setInt(2, agentId); ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean personExists(int personId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM person WHERE person_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean propertyExists(int propId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM property WHERE property_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, propId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── Generic next-ID helper (replaces 3 separate methods) ──────────────────
    private int nextId(String sql) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }
}
