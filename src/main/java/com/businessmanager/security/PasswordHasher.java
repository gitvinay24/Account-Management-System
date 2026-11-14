package com.businessmanager.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.security.MessageDigest;

/**
 * Secure Password Hashing utility using PBKDF2 with HMAC-SHA256.
 * Zero external native library dependency.
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // 32 bytes
    private static final int SALT_LENGTH = 16;  // 16 bytes

    private PasswordHasher() {
    }

    /**
     * Generates a random 16-byte salt encoded as Base64 string.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 and salt.
     */
    public static String hashPassword(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = factory.generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(hash);
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verifies if a raw password matches the stored salt and password hash.
     */
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        if (password == null || storedHash == null || storedSalt == null) {
            return false;
        }
        try {
            byte[] expected = Base64.getDecoder().decode(storedHash);
            byte[] actual = Base64.getDecoder().decode(hashPassword(password, storedSalt));
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void main(String[] args) {
        String salt = generateSalt();
        String hash = hashPassword("Password@123", salt);
        System.out.println("SALT: " + salt);
        System.out.println("HASH: " + hash);
    }
}
