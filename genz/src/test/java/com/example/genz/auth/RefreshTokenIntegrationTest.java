package com.example.genz.auth;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.RefreshTokenRequest;
import com.example.genz.auth.entity.RefreshToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.RefreshTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.service.PlasGateSmsSender;
import com.example.genz.auth.service.RedisOtpStore;
import com.example.genz.auth.util.TokenHashUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_refresh_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000",
        "otp.secret=test-only-otp-secret-at-least-32-characters"
})
@AutoConfigureMockMvc
@Transactional
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private RedisOtpStore otpStore;

    @MockitoBean
    private PlasGateSmsSender smsSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String phoneNumber = "0987654321";
    private final String password = "Password123!";

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .fullName("Refresh Test User")
                .phoneNumber(phoneNumber)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode(password))
                .pinHash(passwordEncoder.encode("123456"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .energy(100)
                .totalXp(0)
                .build());
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber(phoneNumber);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("refreshToken").asText();
    }

    @Test
    void refresh_WithValidToken_ReturnsNewTokensAndStatus200() throws Exception {
        String initialRefreshToken = loginAndGetRefreshToken();

        // 1. First refresh should succeed
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();

        JsonNode root = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = root.path("data").path("refreshToken").asText();
        assertNotEquals(initialRefreshToken, newRefreshToken);

        // 2. The old refresh token should now be marked revoked in DB
        String oldHash = TokenHashUtil.sha256Hex(initialRefreshToken);
        RefreshToken oldStored = refreshTokenRepository.findByTokenHash(oldHash).orElseThrow();
        assertTrue(oldStored.isRevoked());

        // 3. The new refresh token should be active in DB
        String newHash = TokenHashUtil.sha256Hex(newRefreshToken);
        RefreshToken newStored = refreshTokenRepository.findByTokenHash(newHash).orElseThrow();
        assertFalse(newStored.isRevoked());
    }

    @Test
    void refresh_WithReplayedOldToken_FailsWith401AndRevokesAllSessions() throws Exception {
        String initialRefreshToken = loginAndGetRefreshToken();

        // 1. First refresh succeeds
        RefreshTokenRequest firstRefresh = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefresh)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String secondRefreshToken = root.path("data").path("refreshToken").asText();

        // 2. Replaying the INITIAL (now revoked) token should fail with 401 Unauthorized
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked. All active sessions invalidated."));

        // 3. Reuse detection should have invalidated ALL tokens for this user, including the second token!
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(testUser);
        assertTrue(activeTokens.isEmpty());

        // 4. Therefore, trying to use the second token should now also fail with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_WithTamperedToken_FailsWith401() throws Exception {
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest("header.payload.invalid-signature");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_WithValidToken_SucceedsAndSubsequentRefreshFailsWith401() throws Exception {
        String refreshToken = loginAndGetRefreshToken();

        // 1. Call logout
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 2. Calling refresh with the logged out token should fail with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
