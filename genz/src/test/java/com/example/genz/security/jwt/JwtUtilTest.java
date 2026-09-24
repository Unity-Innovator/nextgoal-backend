package com.example.genz.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void accessAndRefreshTokensCannotBeUsedInterchangeably() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);
        UserDetails user = activeUser("0123456789");

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        assertTrue(jwtUtil.validateToken(accessToken, user));
        assertFalse(jwtUtil.validateToken(refreshToken, user));
        assertTrue(jwtUtil.validateRefreshToken(refreshToken, user));
        assertFalse(jwtUtil.validateRefreshToken(accessToken, user));
    }

    @Test
    void disabledUsersCannotUseEitherTokenType() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);
        UserDetails disabledUser = User.withUsername("0123456789")
                .password("unused")
                .disabled(true)
                .authorities("ROLE_USER")
                .build();

        assertFalse(jwtUtil.validateToken(
                jwtUtil.generateAccessToken(disabledUser.getUsername()),
                disabledUser
        ));
        assertFalse(jwtUtil.validateRefreshToken(
                jwtUtil.generateRefreshToken(disabledUser.getUsername()),
                disabledUser
        ));
    }

    @Test
    void weakSecretsAndInvalidExpirationsFailFast() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 60_000, 120_000));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(SECRET, 0, 120_000));
    }

    @Test
    void reportsConfiguredAccessLifetimeInSeconds() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 90_000, 120_000);
        assertEquals(90, jwtUtil.getAccessExpirationSeconds());
        assertEquals(300, jwtUtil.getStepUpExpirationSeconds());
    }

    @Test
    void stepUpTokenValidatesCorrectlyAndCannotBeUsedAsAccessToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000, 300_000);
        UserDetails user = activeUser("0123456789");

        String stepUpToken = jwtUtil.generateStepUpToken(user.getUsername(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        // Validates as step-up token
        assertTrue(jwtUtil.validateStepUpToken(stepUpToken, user.getUsername(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE));

        // CANNOT be used as normal access token
        assertFalse(jwtUtil.validateToken(stepUpToken, user));

        // CANNOT be used as refresh token
        assertFalse(jwtUtil.validateRefreshToken(stepUpToken, user));
    }

    @Test
    void accessTokenCannotBeUsedAsStepUpToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000, 300_000);
        UserDetails user = activeUser("0123456789");

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        assertFalse(jwtUtil.validateStepUpToken(accessToken, user.getUsername(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE));
    }

    @Test
    void stepUpTokenRejectsWrongUserOrWrongScope() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000, 300_000);

        String stepUpToken = jwtUtil.generateStepUpToken("0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        // Wrong user
        assertFalse(jwtUtil.validateStepUpToken(stepUpToken, "0987654321", JwtUtil.FINANCIAL_TRANSACTION_SCOPE));

        // Wrong scope
        assertFalse(jwtUtil.validateStepUpToken(stepUpToken, "0123456789", "OTHER_SCOPE"));
    }

    @Test
    void stepUpTokenRejectsNullOrBlankOrInvalidSignature() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000, 300_000);
        JwtUtil anotherJwtUtil = new JwtUtil("98765432109876543210987654321098", 60_000, 120_000, 300_000);

        String foreignToken = anotherJwtUtil.generateStepUpToken("0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        assertFalse(jwtUtil.validateStepUpToken(null, "0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE));
        assertFalse(jwtUtil.validateStepUpToken("", "0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE));
        assertFalse(jwtUtil.validateStepUpToken("invalid-token", "0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE));
        assertFalse(jwtUtil.validateStepUpToken(foreignToken, "0123456789", JwtUtil.FINANCIAL_TRANSACTION_SCOPE));
    }

    private UserDetails activeUser(String username) {
        return User.withUsername(username)
                .password("unused")
                .authorities("ROLE_USER")
                .build();
    }
}
