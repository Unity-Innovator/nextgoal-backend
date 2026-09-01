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
    }

    private UserDetails activeUser(String username) {
        return User.withUsername(username)
                .password("unused")
                .authorities("ROLE_USER")
                .build();
    }
}
