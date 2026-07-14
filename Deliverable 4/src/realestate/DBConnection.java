package realestate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton JDBC connection manager.
 * Edit USER / PASSWORD to match your MySQL installation.
 */
public class DBConnection {

    private static final String DB_URL  =
        "jdbc:mysql://localhost:3306/real_estate_db"
        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";   // set your MySQL root password here

    private static Connection connection = null;

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

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("  Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("  Warning: could not close connection — " + e.getMessage());
            }
        }
    }
}
