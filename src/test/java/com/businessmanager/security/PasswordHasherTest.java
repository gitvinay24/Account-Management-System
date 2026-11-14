package com.businessmanager.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void verifiesOnlyTheOriginalPassword() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("Correct#Password1", salt);

        assertTrue(PasswordHasher.verifyPassword("Correct#Password1", hash, salt));
        assertFalse(PasswordHasher.verifyPassword("incorrect", hash, salt));
    }

    @Test
    void usesRandomSalts() {
        String firstSalt = PasswordHasher.generateSalt();
        String secondSalt = PasswordHasher.generateSalt();

        assertNotEquals(firstSalt, secondSalt);
        assertNotEquals(PasswordHasher.hashPassword("Correct#Password1", firstSalt),
                PasswordHasher.hashPassword("Correct#Password1", secondSalt));
    }
}
