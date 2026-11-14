package com.businessmanager.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Initializes database tables and verifies the required assessment account.
 *
 * The application owns a dedicated `business_manager` database. If that database
 * does not exist yet, it is created automatically through a server-level
 * connection when the configured user holds the global CREATE privilege
 * (otherwise run the database/setup.sql bootstrap script once as administrator).
 */
public final class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    private DatabaseInitializer() {
    }

    /**
     * Verifies MySQL connection and ensures schema and the required account exist.
     * Returns true if database is ready, false if connection failed.
     */
    public static boolean initializeDatabase() {
        try {
            if (!ensureDatabaseExists()) {
                return false;
            }

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'users'");
                if (!rs.next()) {
                    LOGGER.info("Tables not found. Initializing database schema...");
                    runSqlScript(conn, "database/schema.sql");
                    LOGGER.info("Database schema initialized successfully!");
                } else {
                    LOGGER.info("Database connection established and tables verified.");
                }

                ensureRequiredAdminAccountExists(conn);
            }
            return true;
        } catch (Exception e) {
            LOGGER.severe("Database initialization warning: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts a direct connection to the configured database. If MySQL answers
     * with "unknown database", creates the database through the server URL and
     * retries. Returns false when the server itself is unreachable.
     */
    private static boolean ensureDatabaseExists() {
        try (Connection ignored = DBConnection.getConnection()) {
            return true;
        } catch (SQLException unknownDatabase) {
            if (!isUnknownDatabaseError(unknownDatabase)) {
                return false; // server unreachable or credentials rejected
            }
            LOGGER.info("Database does not exist yet. Creating it automatically...");
            try (Connection server = DBConnection.getServerConnection();
                 Statement stmt = server.createStatement()) {
                String databaseName = DBConnection.getDatabaseName();
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                LOGGER.info("Database '" + databaseName + "' created successfully.");
                return true;
            } catch (SQLException creationFailure) {
                LOGGER.severe("Could not create database automatically: " + creationFailure.getMessage());
                return false;
            }
        }
    }

    private static boolean isUnknownDatabaseError(SQLException exception) {
        // MySQL error 1049: Unknown database
        while (exception != null) {
            if (exception.getErrorCode() == 1049) {
                return true;
            }
            exception = exception.getNextException();
        }
        return false;
    }

    private static void ensureRequiredAdminAccountExists(Connection conn) {
        try {
            String checkUserSql = "SELECT id FROM users WHERE email = 'admin@businessmanager.com'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkUserSql)) {
                if (rs.next()) {
                    return;
                }
            }

            LOGGER.info("Creating required administrator account (admin@businessmanager.com)...");

            int businessId = 0;
            String checkBizSql = "SELECT id FROM businesses WHERE email = 'admin@businessmanager.com'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkBizSql)) {
                if (rs.next()) {
                    businessId = rs.getInt("id");
                }
            }

            if (businessId == 0) {
                String insertBizSql = "INSERT INTO businesses (name, type, registration_number, tax_id, email, phone, address, city, state, postal_code, country) " +
                        "VALUES ('Account Management', 'SMALL', NULL, NULL, 'admin@businessmanager.com', NULL, NULL, NULL, NULL, NULL, 'India')";
                try (PreparedStatement pstmt = conn.prepareStatement(insertBizSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            businessId = keys.getInt(1);
                        }
                    }
                }
            }

            int branchId = 0;
            String checkBranchSql = "SELECT id FROM branches WHERE business_id = ? AND (name = 'Main Branch' OR is_main = 1)";
            try (PreparedStatement pstmt = conn.prepareStatement(checkBranchSql)) {
                pstmt.setInt(1, businessId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        branchId = rs.getInt("id");
                    }
                }
            }

            if (branchId == 0) {
                String insertBranchSql = "INSERT INTO branches (business_id, name, code, phone, email, address, city, state, is_main, active) " +
                        "VALUES (?, 'Main Branch', 'MAIN', NULL, NULL, NULL, NULL, NULL, 1, 1)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertBranchSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, businessId);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            branchId = keys.getInt(1);
                        }
                    }
                }
            }

            String salt = com.businessmanager.security.PasswordHasher.generateSalt();
            String passwordHash = com.businessmanager.security.PasswordHasher.hashPassword("admin123", salt);

            String insertUserSql = "INSERT INTO users (business_id, branch_id, full_name, username, email, phone, password_hash, salt, role, active) " +
                    "VALUES (?, ?, 'Account Management Administrator', 'admin', 'admin@businessmanager.com', NULL, ?, ?, 'ADMIN', 1)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql)) {
                pstmt.setInt(1, businessId);
                if (branchId > 0) {
                    pstmt.setInt(2, branchId);
                } else {
                    pstmt.setNull(2, java.sql.Types.INTEGER);
                }
                pstmt.setString(3, passwordHash);
                pstmt.setString(4, salt);
                pstmt.executeUpdate();
            }

            LOGGER.info("Required administrator account created and verified.");
        } catch (Exception e) {
            LOGGER.warning("Could not seed sample admin account: " + e.getMessage());
        }
    }

    private static void runSqlScript(Connection conn, String relativePath) throws Exception {
        File file = new File(relativePath);
        String sql = "";
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            sql = sb.toString();
        } else {
            InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream(relativePath);
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                sql = sb.toString();
            } else {
                throw new IllegalStateException("SQL script not found: " + relativePath);
            }
        }

        if (!sql.trim().isEmpty()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }
}
