package com.businessmanager.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connection pool / single connections to MySQL.
 */
public final class DBConnection {

    private static String dbUrl = "";
    private static String dbUser = "";
    private static String dbPassword = "";
    private static String serverUrl = "";

    static {
        loadConfig();
    }

    private DBConnection() {
    }

    private static void loadConfig() {
        try (InputStream is = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                if (props.containsKey("db.url")) dbUrl = props.getProperty("db.url");
                if (props.containsKey("db.user")) dbUser = props.getProperty("db.user");
                if (props.containsKey("db.password")) dbPassword = props.getProperty("db.password");
                if (props.containsKey("db.serverUrl")) serverUrl = props.getProperty("db.serverUrl");
            }
        } catch (Exception e) {
            System.err.println("Could not load db.properties, using default configuration: " + e.getMessage());
        }

        // Environment variable overrides if present
        String envUrl = System.getenv("BUSINESS_MANAGER_DB_URL");
        String envUser = System.getenv("BUSINESS_MANAGER_DB_USER");
        String envPass = System.getenv("BUSINESS_MANAGER_DB_PASS");

        if (envUrl != null && !envUrl.trim().isEmpty()) dbUrl = envUrl;
        if (envUser != null && !envUser.trim().isEmpty()) dbUser = envUser;
        if (envPass != null) dbPassword = envPass;

        // The server URL (connection without a default database) is derived from
        // db.url unless an explicit db.serverUrl is configured.
        if (serverUrl == null || serverUrl.isBlank()) {
            serverUrl = deriveServerUrl(dbUrl);
        }
    }

    /**
     * Strips the database name (and any trailing path segments) from a JDBC URL
     * so the result connects to the server without selecting a database.
     */
    private static String deriveServerUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int schemeEnd = url.indexOf("//");
        int pathStart = schemeEnd < 0 ? -1 : url.indexOf('/', schemeEnd + 2);
        if (pathStart < 0) {
            return url; // URL has no database segment to strip
        }
        // Keep everything up to and including the slash that follows host[:port];
        // dropping the database name and parameters yields a server-level URL
        // such as jdbc:mysql://127.0.0.1:3306/ with no default database selected.
        return url.substring(0, pathStart + 1);
    }

    /**
     * Returns the database name from the configured JDBC URL, without URL-decoding.
     */
    public static String getDatabaseName() {
        if (dbUrl == null || dbUrl.isBlank()) {
            return "";
        }
        int schemeEnd = dbUrl.indexOf("//");
        int pathStart = schemeEnd < 0 ? -1 : dbUrl.indexOf('/', schemeEnd + 2);
        if (pathStart < 0) {
            return "";
        }
        String remainder = dbUrl.substring(pathStart + 1);
        int cut = remainder.length();
        for (char separator : new char[]{'?', ';', '/'}) {
            int index = remainder.indexOf(separator);
            if (index >= 0 && index < cut) {
                cut = index;
            }
        }
        return remainder.substring(0, cut);
    }

    /**
     * Obtains a connection to the target database.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on classpath", e);
        }
        validateDatabaseConfiguration(dbUrl);
        try {
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException exception) {
            throw new SQLException("Unable to connect to the Account Management database. Please verify MySQL is running and database credentials are configured.", exception.getSQLState(), exception.getErrorCode(), exception);
        }
    }

    /**
     * Obtains a server connection.
     */
    public static Connection getServerConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on classpath", e);
        }
        validateDatabaseConfiguration(serverUrl);
        try {
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(serverUrl, dbUser, dbPassword);
        } catch (SQLException exception) {
            throw new SQLException("Unable to connect to the Account Management MySQL server. Please verify the server configuration and credentials.", exception.getSQLState(), exception.getErrorCode(), exception);
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    private static void validateDatabaseConfiguration(String url) throws SQLException {
        if (url == null || url.isBlank() || dbUser == null || dbUser.isBlank()) {
            throw new SQLException("Database configuration is incomplete. Set db.url and db.user in db.properties or BUSINESS_MANAGER_DB_* environment variables.");
        }
    }
}
