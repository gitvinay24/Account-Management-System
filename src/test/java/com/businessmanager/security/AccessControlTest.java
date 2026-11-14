package com.businessmanager.security;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessControlTest {
    @AfterEach void clear() { UserSession.clearSession(); }

    @Test
    void cashierCannotModifyEmployeesButCanCreateCustomers() {
        User cashier = new User(); cashier.setRole(UserRole.CASHIER);
        Business business = new Business(1, "Test", BusinessType.SMALL, "test@example.test");
        UserSession.startSession(cashier, business);
        assertThrows(SecurityException.class, () -> AccessControl.requireWrite("EMPLOYEES"));
        assertDoesNotThrow(() -> AccessControl.requireWrite("CUSTOMERS"));
    }

    @Test
    void ownerCanModifyRestrictedModules() {
        User owner = new User(); owner.setRole(UserRole.OWNER);
        UserSession.startSession(owner, new Business(1, "Test", BusinessType.LARGE, "test@example.test"));
        assertDoesNotThrow(() -> AccessControl.requireWrite("BRANCHES"));
        assertDoesNotThrow(() -> AccessControl.requireWrite("EMPLOYEES"));
    }
}
