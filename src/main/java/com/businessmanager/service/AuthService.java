package com.businessmanager.service;

import com.businessmanager.dao.BusinessDAO;
import com.businessmanager.dao.UserDAO;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.security.PasswordHasher;
import com.businessmanager.security.UserSession;
import com.businessmanager.database.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,100}$");

    private final UserDAO userDAO;
    private final BusinessDAO businessDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.businessDAO = new BusinessDAO();
    }

    public AuthService(UserDAO userDAO, BusinessDAO businessDAO) {
        this.userDAO = userDAO;
        this.businessDAO = businessDAO;
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;
        private final Business business;

        public AuthResult(boolean success, String message, User user, Business business) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.business = business;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
        public Business getBusiness() { return business; }
    }

    public AuthResult login(String identifier, String rawPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return new AuthResult(false, "Please enter your username or email.", null, null);
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return new AuthResult(false, "Please enter your password.", null, null);
        }

        try {
            User user = userDAO.findByUsernameOrEmail(identifier.trim());
            if (user == null) {
                return new AuthResult(false, "Account not found with provided username or email.", null, null);
            }

            if (!user.isActive()) {
                return new AuthResult(false, "This user account is inactive. Please contact your system administrator.", null, null);
            }

            boolean passwordMatch = PasswordHasher.verifyPassword(rawPassword, user.getPasswordHash(), user.getSalt());
            if (!passwordMatch) {
                return new AuthResult(false, "Invalid password. Please try again.", null, null);
            }

            Business business = businessDAO.findById(user.getBusinessId());
            if (business == null) {
                return new AuthResult(false, "Associated business account not found.", null, null);
            }

            UserSession.startSession(user, business);
            return new AuthResult(true, "Login successful!", user, business);

        } catch (SQLException e) {
            return new AuthResult(false, "Database connection error during login: " + e.getMessage(), null, null);
        }
    }

    public void logout() {
        UserSession.clearSession();
    }

    public AuthResult registerBusinessAndUser(User newUser, Business newBusiness, char[] rawPassword) {
        String validationError = validateRegistration(newUser, newBusiness, rawPassword);
        if (validationError != null) return new AuthResult(false, validationError, null, null);

        String password = new String(rawPassword);
        try {
            if (userDAO.existsByEmail(newUser.getEmail().trim())) {
                return new AuthResult(false, "An account with this email already exists.", null, null);
            }

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int businessId = businessDAO.create(conn, newBusiness);
                    if (businessId <= 0) throw new SQLException("Business record was not created.");
                    newBusiness.setId(businessId);

                    String salt = PasswordHasher.generateSalt();
                    newUser.setBusinessId(businessId);
                    newUser.setPasswordHash(PasswordHasher.hashPassword(password, salt));
                    newUser.setSalt(salt);
                    if (newUser.getRole() == null) newUser.setRole(UserRole.OWNER);
                    newUser.setActive(true);

                    int userId = userDAO.create(conn, newUser);
                    if (userId <= 0) throw new SQLException("User account was not created.");
                    newUser.setId(userId);
                    conn.commit();
                } catch (SQLException | RuntimeException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
            UserSession.startSession(newUser, newBusiness);
            return new AuthResult(true, "Business and owner account created successfully!", newUser, newBusiness);
        } catch (SQLException e) {
            return new AuthResult(false, "Registration could not be completed. Please check your details and database connection.", null, null);
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
    }

    private String validateRegistration(User user, Business business, char[] password) {
        if (user == null || business == null) return "Registration details are missing.";
        if (isBlank(user.getFullName())) return "Full name is required.";
        if (isBlank(user.getUsername()) || !USERNAME_PATTERN.matcher(user.getUsername().trim()).matches()) {
            return "Username must be 3–100 characters and use letters, numbers, dots, hyphens, or underscores only.";
        }
        if (isBlank(user.getEmail()) || !EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) return "Enter a valid email address.";
        if (isBlank(business.getName())) return "Business name is required.";
        if (isBlank(business.getEmail()) || !EMAIL_PATTERN.matcher(business.getEmail().trim()).matches()) return "Enter a valid business email address.";
        if (business.getType() == null) return "Select a business type.";
        if (password == null || password.length < 8) return "Password must be at least 8 characters long.";
        String candidate = new String(password);
        boolean strong = candidate.chars().anyMatch(Character::isUpperCase)
                && candidate.chars().anyMatch(Character::isLowerCase)
                && candidate.chars().anyMatch(Character::isDigit)
                && candidate.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        return strong ? null : "Password must include upper-case, lower-case, number, and symbol characters.";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public AuthResult changePassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return new AuthResult(false, "New password must be at least 6 characters.", null, null);
        }

        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                return new AuthResult(false, "User not found.", null, null);
            }

            if (!PasswordHasher.verifyPassword(oldPassword, user.getPasswordHash(), user.getSalt())) {
                return new AuthResult(false, "Current password is incorrect.", null, null);
            }

            String newSalt = PasswordHasher.generateSalt();
            String newHash = PasswordHasher.hashPassword(newPassword, newSalt);

            boolean updated = userDAO.updatePassword(userId, newHash, newSalt);
            if (updated) {
                user.setPasswordHash(newHash);
                user.setSalt(newSalt);
                return new AuthResult(true, "Password changed successfully!", user, UserSession.getCurrentBusiness());
            } else {
                return new AuthResult(false, "Failed to update password.", null, null);
            }
        } catch (SQLException e) {
            return new AuthResult(false, "Database error: " + e.getMessage(), null, null);
        }
    }
}
