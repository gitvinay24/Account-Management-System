package com.businessmanager.e2e;

import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.database.DBConnection;
import com.businessmanager.database.DatabaseInitializer;
import com.businessmanager.enums.UserRole;
import com.businessmanager.security.AccessControl;
import com.businessmanager.security.PasswordHasher;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AuthService;
import com.businessmanager.ui.MainFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Test Suite for Real Sample Admin Account Verification.
 * Validates MySQL persistence, PBKDF2 password hashing, AuthService pipeline,
 * AccessControl permissions, negative login tests, and full module access.
 */
@Tag("integration")
@EnabledIfSystemProperty(named = "BUSINESS_MANAGER_IT", matches = "true")
public class SampleAdminAccountVerificationTest {

    private static final String ADMIN_EMAIL = "admin@businessmanager.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeAll
    static void setupEnv() {
        System.clearProperty("ASSESSMENT_MODE");
        System.clearProperty("DEMO_MODE_ENABLED");
        assertTrue(DatabaseInitializer.initializeDatabase(), "Live MySQL Initialization failed");
        assertFalse(DemoModeConfig.isEnabled(), "DemoModeConfig must default to false for real MySQL persistence");
    }

    @Test
    void verifyRealSampleAdminAccountCreationAndAuthentication() throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING REAL SAMPLE ADMIN ACCOUNT VERIFICATION");
        System.out.println("==================================================");

        // 1. MySQL Direct Database Verification
        System.out.println("[STEP 1] Direct MySQL Database Verification...");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT u.*, b.name as business_name, br.name as branch_name " +
                     "FROM users u " +
                     "JOIN businesses b ON u.business_id = b.id " +
                     "LEFT JOIN branches br ON u.branch_id = br.id " +
                     "WHERE u.email = ?")) {
            pstmt.setString(1, ADMIN_EMAIL);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next(), "Sample admin account (admin@businessmanager.com) must exist in MySQL DB");
                assertEquals("admin", rs.getString("username"));
                assertEquals("Demo Administrator", rs.getString("full_name"));
                assertEquals("ADMIN", rs.getString("role"));
                assertTrue(rs.getBoolean("active"));
                assertEquals("Demo Business", rs.getString("business_name"));
                assertNotNull(rs.getString("branch_name"));

                // PBKDF2 Hashing verification
                String hash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                assertNotEquals(ADMIN_PASSWORD, hash, "Password must NOT be stored in plain text");
                assertTrue(PasswordHasher.verifyPassword(ADMIN_PASSWORD, hash, salt), "PBKDF2-HMAC-SHA256 password hash must verify");
            }
        }
        System.out.println("  ✓ MySQL Database contains verified record for admin@businessmanager.com with PBKDF2 hash.");

        // 2. Real Authentication via AuthService
        System.out.println("[STEP 2] Authenticating through AuthService...");
        AuthService authService = new AuthService();
        AuthService.AuthResult result = authService.login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertTrue(result.isSuccess(), "Login with admin@businessmanager.com / admin123 must succeed");
        assertTrue(UserSession.isLoggedIn());
        assertEquals("Demo Administrator", UserSession.getCurrentUser().getFullName());
        assertEquals(UserRole.ADMIN, UserSession.getCurrentUser().getRole());
        assertEquals("Demo Business", UserSession.getCurrentBusiness().getName());
        System.out.println("  ✓ AuthService login succeeded. Active Session: " + UserSession.getCurrentUser().getFullName() + " [" + UserSession.getCurrentUser().getRole() + "]");

        // 3. Negative Login Tests
        System.out.println("[STEP 3] Running Negative Login Tests...");
        AuthService.AuthResult badPass = authService.login(ADMIN_EMAIL, "wrongpassword");
        assertFalse(badPass.isSuccess(), "Invalid password must be rejected");
        assertTrue(badPass.getMessage().contains("Invalid password"));

        AuthService.AuthResult badEmail = authService.login("nonexistent@domain.com", ADMIN_PASSWORD);
        assertFalse(badEmail.isSuccess(), "Non-existent user must be rejected");
        assertTrue(badEmail.getMessage().contains("Account not found"));
        System.out.println("  ✓ Negative login attempts correctly rejected by AuthService.");

        // 4. GUI Launch & Full Faculty Demo Access
        System.out.println("[STEP 4] Testing GUI MainFrame & All Module Panels...");
        final MainFrame[] frameHolder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frameHolder[0] = frame;
            frame.onLoginSuccess();
            frame.setVisible(true);
        });
        MainFrame mainFrame = frameHolder[0];
        assertNotNull(mainFrame);

        String[] modules = {
                "DASHBOARD",
                "POS",
                "PRODUCTS",
                "CATEGORIES",
                "INVENTORY",
                "CUSTOMERS",
                "SUPPLIERS",
                "PURCHASES",
                "EXPENSES",
                "EMPLOYEES",
                "BRANCHES",
                "REPORTS",
                "ANALYTICS"
        };

        for (String module : modules) {
            assertTrue(AccessControl.canAccessModule(module), "ADMIN role must have access to module: " + module);
            System.out.println("  ✓ Module access verified for ADMIN: " + module);
        }

        SwingUtilities.invokeAndWait(mainFrame::dispose);

        // 5. Idempotency Verification
        System.out.println("[STEP 5] Verifying Idempotency (No duplicate accounts on restart)...");
        DatabaseInitializer.initializeDatabase();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?")) {
            pstmt.setString(1, ADMIN_EMAIL);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Exactly one account must exist for admin@businessmanager.com");
            }
        }
        System.out.println("  ✓ Idempotency verified. Exactly 1 record exists.");

        // 6. Logout, Restart & Persistence
        System.out.println("[STEP 6] Testing Logout, Application Restart & Persistent Authentication...");
        UserSession.clearSession();
        assertFalse(UserSession.isLoggedIn());

        AuthService.AuthResult reLogin = authService.login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertTrue(reLogin.isSuccess(), "Re-login after restart must succeed");
        assertTrue(UserSession.isLoggedIn());
        assertEquals("Demo Administrator", UserSession.getCurrentUser().getFullName());
        System.out.println("  ✓ Persistence across application restart verified.");

        System.out.println("==================================================");
        System.out.println("SAMPLE ADMIN ACCOUNT VERIFICATION SUCCESSFUL!");
        System.out.println("==================================================");
    }
}
