package com.example.shifter.util.login;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class TokenBlacklistTest {
    private TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklist();
    }

    @Test
    void testBlacklistToken() {
        String token = "test-token-123";
        tokenBlacklist.blacklistToken(token);
        assertTrue(tokenBlacklist.isBlacklisted(token));
    }

    @Test
    void testIsBlacklistedReturnsFalseForNonBlacklistedToken() {
        String token = "non-blacklisted-token";
        assertFalse(tokenBlacklist.isBlacklisted(token));
    }

    @Test
    void testMultipleTokens() {
        String token1 = "token-1";
        String token2 = "token-2";
        
        tokenBlacklist.blacklistToken(token1);
        tokenBlacklist.blacklistToken(token2);
        
        assertTrue(tokenBlacklist.isBlacklisted(token1));
        assertTrue(tokenBlacklist.isBlacklisted(token2));
    }

    @Test
    void testBlacklistSameTokenMultipleTimes() {
        String token = "duplicate-token";
        tokenBlacklist.blacklistToken(token);
        tokenBlacklist.blacklistToken(token);
        assertTrue(tokenBlacklist.isBlacklisted(token));
    }
}