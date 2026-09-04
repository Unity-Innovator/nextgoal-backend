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
    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    public static final String STEP_UP_TOKEN_TYPE = "PIN_STEP_UP";
    public static final String SCOPE_CLAIM = "scope";
    public static final String FINANCIAL_TRANSACTION_SCOPE = "FINANCIAL_TRANSACTION";

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final long stepUpExpiration;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration:900000}") long accessExpiration,
                   @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration,
                   @Value("${jwt.step-up-expiration:300000}") long stepUpExpiration) {
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
        if (accessExpiration <= 0 || refreshExpiration <= 0 || stepUpExpiration <= 0) {
            throw new IllegalStateException("JWT expiration values must be positive");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.stepUpExpiration = stepUpExpiration;
    }

    public JwtUtil(String secret, long accessExpiration, long refreshExpiration) {
        this(secret, accessExpiration, refreshExpiration, 300_000L);
    }

    public String generateAccessToken(@NotBlank String phoneNumber) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
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

    public String generateStepUpToken(@NotBlank String phoneNumber, @NotBlank String scope) {
        return Jwts.builder()
                .subject(phoneNumber)
                .claim(TOKEN_TYPE_CLAIM, STEP_UP_TOKEN_TYPE)
                .claim(SCOPE_CLAIM, scope)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + stepUpExpiration))
                .signWith(key)
                .compact();
    }

    public String extractPhoneNumber(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && claims.getSubject().equals(userDetails.getUsername())
                    && userDetails.isEnabled();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && claims.getSubject().equals(userDetails.getUsername())
                    && userDetails.isEnabled();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateStepUpToken(String token, String expectedPhoneNumber, String requiredScope) {
        if (token == null || token.isBlank() || expectedPhoneNumber == null || expectedPhoneNumber.isBlank()) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            String tokenScope = claims.get(SCOPE_CLAIM, String.class);
            String subject = claims.getSubject();

            return STEP_UP_TOKEN_TYPE.equals(tokenType)
                    && expectedPhoneNumber.equals(subject)
                    && (requiredScope == null || requiredScope.equals(tokenScope));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessExpirationSeconds() {
        return accessExpiration / 1_000;
    }

    public long getStepUpExpirationSeconds() {
        return stepUpExpiration / 1_000;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
