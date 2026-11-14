package com.businessmanager.security;

import com.businessmanager.enums.UserRole;

/** Central role policy for application modules. Sensitive write operations must also enforce this in their service. */
public final class AccessControl {
    private AccessControl() { }

    public static boolean canAccessModule(String module) {
        if (!UserSession.isLoggedIn()) return false;
        if (UserSession.isGuestSession()) {
            return switch (module) {
                case "DASHBOARD", "POS", "PRODUCTS", "INVENTORY", "CUSTOMERS", "SUPPLIERS", "SALES", "REPORTS", "ANALYTICS" -> true;
                default -> false;
            };
        }
        if (UserSession.hasRole(UserRole.ADMIN, UserRole.OWNER)) return true;
        UserRole role = UserSession.getCurrentUser().getRole();
        return switch (role) {
            case MANAGER -> !"EMPLOYEES".equals(module) && !"SETTINGS".equals(module);
            case CASHIER -> switch (module) {
                case "DASHBOARD", "POS", "SALES", "CUSTOMERS", "PAYMENTS" -> true;
                default -> false;
            };
            case INVENTORY_MANAGER -> switch (module) {
                case "DASHBOARD", "PRODUCTS", "CATEGORIES", "INVENTORY", "SUPPLIERS", "PURCHASES" -> true;
                default -> false;
            };
            default -> false;
        };
    }

    /** Enforces write access even when a caller bypasses Swing navigation. */
    public static void requireWrite(String module) {
        if (UserSession.isGuestSession()) {
            throw new SecurityException("Guest sample access is read-only. Log in to modify " + module.toLowerCase() + ".");
        }
        if (!canAccessModule(module)) throw new SecurityException("You do not have permission to modify " + module.toLowerCase() + ".");
        if (UserSession.hasRole(UserRole.ADMIN, UserRole.OWNER, UserRole.MANAGER)) return;
        UserRole role = UserSession.getCurrentUser().getRole();
        boolean allowed = switch (role) {
            case CASHIER -> "CUSTOMERS".equals(module) || "PAYMENTS".equals(module) || "POS".equals(module);
            case INVENTORY_MANAGER -> "PRODUCTS".equals(module) || "CATEGORIES".equals(module) || "INVENTORY".equals(module) || "SUPPLIERS".equals(module) || "PURCHASES".equals(module);
            default -> false;
        };
        if (!allowed) throw new SecurityException("You do not have permission to modify " + module.toLowerCase() + ".");
    }
}
