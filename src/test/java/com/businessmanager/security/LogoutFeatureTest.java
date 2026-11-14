package com.businessmanager.security;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogoutFeatureTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        UserSession.clearSession();
    }

    @AfterEach
    void tearDown() {
        UserSession.clearSession();
    }

    @Test
    void testLogoutClearsSessionState() {
        User user = new User();
        user.setId(101);
        user.setUsername("testuser");
        user.setRole(UserRole.ADMIN);

        Business business = new Business(1, "Test Business", BusinessType.SMALL, "owner@test.com");
        UserSession.startSession(user, business);

        assertTrue(UserSession.isLoggedIn());
        assertNotNull(UserSession.getCurrentUser());
        assertNotNull(UserSession.getCurrentBusiness());
        assertEquals(101, UserSession.getUserId());
        assertEquals(1, UserSession.getBusinessId());

        // Perform logout
        authService.logout();

        assertFalse(UserSession.isLoggedIn());
        assertNull(UserSession.getCurrentUser());
        assertNull(UserSession.getCurrentBusiness());
        assertEquals(0, UserSession.getUserId());
        assertEquals(0, UserSession.getBusinessId());
    }

    @Test
    void testLogoutRevokesAccessPermissions() {
        User user = new User();
        user.setRole(UserRole.OWNER);
        Business business = new Business(1, "Enterprise Inc", BusinessType.LARGE, "owner@enterprise.com");

        UserSession.startSession(user, business);
        assertTrue(AccessControl.canAccessModule("DASHBOARD"));

        authService.logout();

        assertFalse(UserSession.isLoggedIn());
        assertFalse(AccessControl.canAccessModule("DASHBOARD"));
        assertThrows(SecurityException.class, () -> AccessControl.requireWrite("PRODUCTS"));
    }

    @Test
    void testIdempotentLogout() {
        assertDoesNotThrow(() -> authService.logout());
        assertFalse(UserSession.isLoggedIn());
        assertDoesNotThrow(() -> authService.logout());
        assertFalse(UserSession.isLoggedIn());
    }
}
