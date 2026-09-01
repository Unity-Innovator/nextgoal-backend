package com.example.genz.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-expiration:900000}") long accessExpiration,
                      @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        String normalizedSecret = secret.toLowerCase(Locale.ROOT);
        if (normalizedSecret.contains("replace-me")
                || normalizedSecret.contains("placeholder")
                || normalizedSecret.contains("change-me")) {
            throw new IllegalStateException("JWT_SECRET must not use a placeholder value");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        if (accessExpiration <= 0 || refreshExpiration <= 0) {
            throw new IllegalStateException("JWT expiration values must be positive");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(@NotBlank String phoneNumber) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+accessExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String phoneNumber) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    public String extractPhoneNumber(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && claims.getSubject().equals(userDetails.getUsername())
                && userDetails.isEnabled();
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && claims.getSubject().equals(userDetails.getUsername())
                && userDetails.isEnabled();
    }

    public long getAccessExpirationSeconds() {
        return accessExpiration / 1_000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
