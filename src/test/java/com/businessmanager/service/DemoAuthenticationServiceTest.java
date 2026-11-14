package com.businessmanager.service;

import com.businessmanager.security.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoAuthenticationServiceTest {
    @AfterEach void clear() { UserSession.clearSession(); System.clearProperty("DEMO_MODE_ENABLED"); }

    @Test
    void createsAnAdminSessionWithoutDatabase() {
        System.setProperty("DEMO_MODE_ENABLED", "true");
        assertTrue(new DemoAuthenticationService().authenticate(DemoAuthenticationService.EMAIL, DemoAuthenticationService.PASSWORD));
        assertTrue(UserSession.isDemoMode());
        assertTrue(UserSession.hasRole(com.businessmanager.enums.UserRole.ADMIN));
    }

    @Test
    void rejectsInvalidCredentialsLocally() {
        assertFalse(new DemoAuthenticationService().authenticate(DemoAuthenticationService.EMAIL, "wrong"));
        assertFalse(UserSession.isLoggedIn());
    }
}
