package com.businessmanager.security;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;

/**
 * Global Thread-safe Session Manager.
 */
public final class UserSession {

    private static User currentUser;
    private static Business currentBusiness;
    private static boolean demoMode;
    private static boolean guestSession;

    private UserSession() {
    }

    public static synchronized void startSession(User user, Business business) {
        currentUser = user;
        currentBusiness = business;
        demoMode = user != null && user.getId() == -1;
        guestSession = user != null && user.getRole() == UserRole.GUEST;
    }

    /** Starts a non-persistent, read-only session backed by in-memory sample data. */
    public static synchronized void startGuestSession() {
        User guest = new User();
        guest.setId(-2);
        guest.setBusinessId(-1);
        guest.setFullName("Guest Explorer");
        guest.setUsername("guest");
        guest.setRole(UserRole.GUEST);
        guest.setActive(true);
        startSession(guest, new Business(-1, "Sample Business", BusinessType.LARGE, null));
    }

    public static synchronized void clearSession() {
        currentUser = null;
        currentBusiness = null;
        demoMode = false;
        guestSession = false;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null && currentBusiness != null;
    }

    /** TEMPORARY ASSESSMENT DEMO MODE: allows UI panels to present safe empty states. */
    public static synchronized boolean isDemoMode() { return demoMode; }

    public static synchronized boolean isGuestSession() { return guestSession; }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized Business getCurrentBusiness() {
        return currentBusiness;
    }

    public static synchronized boolean isLargeBusiness() {
        return currentBusiness != null && currentBusiness.getType() == BusinessType.LARGE;
    }

    public static synchronized boolean isSmallBusiness() {
        return currentBusiness != null && currentBusiness.getType() == BusinessType.SMALL;
    }

    public static synchronized boolean hasRole(UserRole... roles) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        for (UserRole role : roles) {
            if (currentUser.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    public static synchronized int getBusinessId() {
        return currentBusiness != null ? currentBusiness.getId() : 0;
    }

    public static synchronized Integer getBranchId() {
        return currentUser != null ? currentUser.getBranchId() : null;
    }

    public static synchronized int getUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }
}
