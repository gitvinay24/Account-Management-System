package com.businessmanager.security;

import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.dao.ProductDAO;
import com.businessmanager.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuestSessionTest {

    @AfterEach
    void clearSession() {
        UserSession.clearSession();
    }

    @Test
    void guestUsesSampleDataAndCannotWrite() throws Exception {
        UserSession.startGuestSession();

        assertTrue(UserSession.isLoggedIn());
        assertTrue(UserSession.isGuestSession());
        assertEquals(UserRole.GUEST, UserSession.getCurrentUser().getRole());
        assertTrue(DemoModeConfig.isEnabled());
        assertFalse(new ProductDAO().findByBusiness(UserSession.getBusinessId()).isEmpty());
        for (String module : new String[]{"DASHBOARD", "POS", "PRODUCTS", "INVENTORY", "CUSTOMERS", "SUPPLIERS", "SALES", "REPORTS", "ANALYTICS"}) {
            assertTrue(AccessControl.canAccessModule(module), module + " should be available to a guest");
        }
        for (String module : new String[]{"SETTINGS", "EMPLOYEES", "BRANCHES", "PAYMENTS", "EXPENSES", "PURCHASES", "CATEGORIES"}) {
            assertFalse(AccessControl.canAccessModule(module), module + " should be restricted for a guest");
        }
        assertThrows(SecurityException.class, () -> AccessControl.requireWrite("PRODUCTS"));
    }
}
