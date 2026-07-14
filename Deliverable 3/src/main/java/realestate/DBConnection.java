package realestate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton helper that opens and caches one JDBC connection.
 * Edit DB_URL / USER / PASSWORD to match your MySQL installation.
 */
public class DBConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/real_estate_db"
                                        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";   // change if you have a MySQL root password

    private static Connection connection = null;

    /** Returns (and caches) the single JDBC connection. */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. "
                        + "Ensure mysql-connector-j-*.jar is on the classpath.", e);
            }
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        }
        return connection;
    }

    /** Closes the cached connection gracefully. */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Warning: could not close connection – " + e.getMessage());
            }
        }
    }
}
