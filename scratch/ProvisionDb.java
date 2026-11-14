import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One-shot provisioning helper (deleted after use): creates business_manager
 * and the dedicated app user, reusing an existing local dev credential.
 */
public class ProvisionDb {

    public static void main(String[] args) {
        String serverUrl = "jdbc:mysql://127.0.0.1:3306/?allowMultiQueries=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String bootstrapUser = "retailpro_user";
        String bootstrapPass = "Rp!2026_local_9fQ2vK7xA4mD";

        try (Connection conn = DriverManager.getConnection(serverUrl, bootstrapUser, bootstrapPass)) {
            System.out.println("Connected to MySQL server as " + bootstrapUser);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE IF NOT EXISTS `business_manager` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("Database business_manager ready");
                stmt.execute("CREATE USER IF NOT EXISTS 'business_manager_app'@'localhost' IDENTIFIED BY 'Bm!Local2026_Db9x'");
                stmt.execute("ALTER USER 'business_manager_app'@'localhost' IDENTIFIED BY 'Bm!Local2026_Db9x'");
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON `business_manager`.* TO 'business_manager_app'@'localhost'");
                stmt.execute("FLUSH PRIVILEGES");
                System.out.println("User business_manager_app ready and granted");
            }
        } catch (SQLException e) {
            System.out.println("BOOTSTRAP FAILED: sqlState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage());
            System.exit(2);
        }

        // Verify the new dedicated user can connect to the new database
        String appUrl = "jdbc:mysql://127.0.0.1:3306/business_manager?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(appUrl, "business_manager_app", "Bm!Local2026_Db9x")) {
            System.out.println("VERIFIED: business_manager_app can connect to business_manager");
        } catch (SQLException e) {
            System.out.println("VERIFY FAILED: sqlState=" + e.getSQLState() + " errorCode=" + e.getErrorCode() + " message=" + e.getMessage());
            System.exit(3);
        }
    }
}
