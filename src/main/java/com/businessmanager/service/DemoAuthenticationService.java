package com.businessmanager.service;

import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.security.UserSession;

/** Local assessment-mode authentication that deliberately avoids JDBC access. */
public final class DemoAuthenticationService {
    public static final String EMAIL = "admin@businessmanager.com";
    public static final String PASSWORD = "admin123";

    public boolean authenticate(String email, String password) {
        if (!DemoModeConfig.isEnabled()
                || !EMAIL.equalsIgnoreCase(email == null ? "" : email.trim())
                || !PASSWORD.equals(password)) {
            UserSession.clearSession();
            return false;
        }
        User user = new User();
        user.setId(-1);
        user.setBusinessId(-1);
        user.setFullName("Demo Administrator");
        user.setUsername("demo_admin");
        user.setEmail(EMAIL);
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        Business business = new Business(-1, "Account Management Demo", BusinessType.LARGE, EMAIL);
        UserSession.startSession(user, business);
        return true;
    }
}
