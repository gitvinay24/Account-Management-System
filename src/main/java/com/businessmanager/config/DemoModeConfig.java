package com.businessmanager.config;

import com.businessmanager.security.UserSession;

/** Assessment mode configuration. Default is false to use live MySQL JDBC persistence. */
public final class DemoModeConfig {
    private DemoModeConfig() { }

    public static boolean isEnabled() {
        if (UserSession.isGuestSession()) return true;
        String value = System.getProperty("ASSESSMENT_MODE", System.getProperty("DEMO_MODE_ENABLED",
                System.getenv().getOrDefault("ASSESSMENT_MODE", System.getenv().getOrDefault("DEMO_MODE_ENABLED", "false"))));
        return Boolean.parseBoolean(value);
    }
}
