
package com.umhamza.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTests {

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    private static final String SAMPLE_HASH =
            "$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS";

    @Test
    void shouldMatchExistingSamplePasswordHash() {
        assertTrue(
                encoder.matches("Password123!", SAMPLE_HASH),
                "BCrypt should recognize the existing sample password hash."
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {
        assertFalse(
                encoder.matches("WrongPassword123!", SAMPLE_HASH),
                "BCrypt should reject an incorrect password."
        );
    }
}
