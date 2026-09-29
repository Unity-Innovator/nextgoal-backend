package com.example.genz.auth;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_pin_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@AutoConfigureMockMvc
@Transactional
class PinVerificationAndFinancialStepUpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.genz.wallet.repository.WalletRepository walletRepository;

    private User userA;
    private User userB;
    private User userNoPin;
    private Goal goalA;

    @BeforeEach
    void setUp() {
        userA = userRepository.saveAndFlush(User.builder()
                .fullName("User Alpha")
                .phoneNumber("0111111111")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        walletRepository.saveAndFlush(com.example.genz.wallet.entity.Wallet.builder()
                .user(userA)
                .balance(new BigDecimal("200.00"))
                .currency(com.example.genz.transaction.enums.Currency.USD)
                .build());

        userB = userRepository.saveAndFlush(User.builder()
                .fullName("User Beta")
                .phoneNumber("0222222222")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("654321"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        userNoPin = userRepository.saveAndFlush(User.builder()
                .fullName("User No Pin")
                .phoneNumber("0333333333")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(null)
                .status("ACTIVE")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        goalA = new Goal();
        goalA.setGoalName("Vacation Fund");
        goalA.setTargetAmount(BigDecimal.valueOf(1000));
        goalA.setCurrentAmount(BigDecimal.ZERO);
        goalA.setTargetDate(LocalDate.now().plusMonths(6));
        goalA.setCreatedAt(LocalDateTime.now());
        goalA.setOwner(userA);
        goalA.setCategory(GoalCategory.TRAVEL);
        goalA = goalRepository.saveAndFlush(goalA);

        GoalMember member = new GoalMember();
        member.setGoal(goalA);
        member.setUser(userA);
        member.setRole(MemberRole.OWNER);
        member.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(member);

        GrouplWallet wallet = new GrouplWallet();
        wallet.setUser(userA);
        wallet.setGoal(goalA);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency(com.example.genz.transaction.enums.Currency.USD);
        goalWalletRepository.saveAndFlush(wallet);
    }

    // ==========================================
    // PIN VERIFICATION ENDPOINT TESTS
    // ==========================================

    @Test
    @DisplayName("PIN verification: Correct PIN with valid access token returns short-lived step-up token")
    void verifyPin_CorrectPin_ReturnsStepUpToken() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());

        MvcResult result = mockMvc.perform(post("/api/auth/verify-pin")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": \"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("PIN verified successfully"))
                .andExpect(jsonPath("$.data.stepUpToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(300))
                .andExpect(jsonPath("$.data.tokenType").value("Step-Up"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String stepUpToken = json.get("data").get("stepUpToken").asText();

        // Verify that the step-up token is valid for userA with FINANCIAL_TRANSACTION scope
        assertTrue(jwtUtil.validateStepUpToken(stepUpToken, userA.getPhoneNumber(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE));

        // Step-up token must NOT be accepted as normal access token
        assertFalse(jwtUtil.validateToken(stepUpToken, userA));
    }

    @Test
    @DisplayName("PIN verification: Incorrect PIN returns 400 Bad Request")
    void verifyPin_IncorrectPin_ReturnsBadRequest() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());

        mockMvc.perform(post("/api/auth/verify-pin")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": \"999999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid PIN"));
    }

    @Test
    @DisplayName("PIN verification: Missing PIN returns 400 Validation Error")
    void verifyPin_MissingPin_ReturnsValidationError() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());

        mockMvc.perform(post("/api/auth/verify-pin")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PIN verification: User without configured PIN returns 400 Bad Request")
    void verifyPin_UserWithoutConfiguredPin_ReturnsBadRequest() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userNoPin.getPhoneNumber());

        mockMvc.perform(post("/api/auth/verify-pin")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": \"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User does not have a PIN configured"));
    }

    @Test
    @DisplayName("PIN verification: Unauthenticated request (no Bearer token) is rejected with 401/403")
    void verifyPin_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(post("/api/auth/verify-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": \"123456\"}"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // SENSITIVE FINANCIAL ENDPOINT TESTS
    // ==========================================

    @Test
    @DisplayName("Financial endpoint: Valid access token + valid step-up token succeeds")
    void financialAction_ValidAccessAndStepUpToken_Succeeds() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        String stepUpToken = jwtUtil.generateStepUpToken(userA.getPhoneNumber(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Step-Up-Token", stepUpToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    @DisplayName("Financial endpoint: Valid access token without X-Step-Up-Token header is rejected")
    void financialAction_MissingStepUpToken_Rejected() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing X-Step-Up-Token header"));
    }

    @Test
    @DisplayName("Financial endpoint: Invalid / corrupted step-up token is rejected")
    void financialAction_InvalidStepUpToken_Rejected() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Step-Up-Token", "invalid-or-tampered-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired step-up token"));
    }

    @Test
    @DisplayName("Financial endpoint: Step-up token belonging to User B used with User A's access token is rejected")
    void financialAction_TokenBelongsToAnotherUser_Rejected() throws Exception {
        String accessTokenA = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        // Step-up token minted for User B
        String stepUpTokenB = jwtUtil.generateStepUpToken(userB.getPhoneNumber(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessTokenA)
                        .header("X-Step-Up-Token", stepUpTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired step-up token"));
    }

    @Test
    @DisplayName("Financial endpoint: Normal access token passed as X-Step-Up-Token is rejected (wrong token type)")
    void financialAction_AccessTokenAsStepUpToken_Rejected() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Step-Up-Token", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired step-up token"));
    }

    @Test
    @DisplayName("Financial endpoint: Expired step-up token is rejected")
    void financialAction_ExpiredStepUpToken_Rejected() throws Exception {
        JwtUtil expiredJwtUtil = new JwtUtil("test-only-jwt-secret-must-be-at-least-32-characters-long", 900000, 604800000, 1);
        String expiredStepUpToken = expiredJwtUtil.generateStepUpToken(userA.getPhoneNumber(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        Thread.sleep(10); // Wait for expiration

        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());
        String depositBody = String.format("{\"goalId\": \"%s\", \"amount\": 100.00}", goalA.getId());

        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Step-Up-Token", expiredStepUpToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired step-up token"));
    }

    // ==========================================
    // NORMAL AUTHENTICATED APIS TEST
    // ==========================================

    @Test
    @DisplayName("Normal API: Regular endpoints continue to work with access token ONLY without step-up token")
    void normalApi_WorksWithAccessTokenOnly() throws Exception {
        String accessToken = jwtUtil.generateAccessToken(userA.getPhoneNumber());

        mockMvc.perform(get("/api/goals")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
