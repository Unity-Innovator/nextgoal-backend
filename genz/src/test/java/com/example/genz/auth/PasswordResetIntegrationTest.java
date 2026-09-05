package com.example.genz.auth;
import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.PasswordResetToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.PasswordResetTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.util.TokenHashUtil;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_pwd_reset_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000",
        "otp.console-delivery-enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String phoneNumber = "0812345678";
    private final String initialPassword = "OldPassword123!";

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .fullName("Reset Test User")
                .phoneNumber(phoneNumber)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode(initialPassword))
                .pinHash(passwordEncoder.encode("123456"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .energy(100)
                .totalXp(0)
                .build());
    }

    @Test
    @DisplayName("Registration OTP verification returns data: null and does not return JWT")
    void registrationOtpVerification_ReturnsNullData_NoJwt() throws Exception {
        String rawOtp = "654321";
        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex(rawOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp(rawOtp);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Complete flow: send OTP -> verify reset OTP -> reset password -> login with new password")
    void fullPasswordResetFlow_Success() throws Exception {
        // 1. Send OTP
        SendOtpRequest sendOtpRequest = new SendOtpRequest();
        sendOtpRequest.setPhoneNumber(phoneNumber);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Retrieve generated OTP from DB for testing
        String rawOtp = "123456";
        Otp otpEntity = otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow();
        // Override hash with known OTP for test determinism
        otpEntity.setOtpHash(TokenHashUtil.sha256Hex(rawOtp));
        otpRepository.save(otpEntity);

        // 2. Verify Reset OTP
        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp(rawOtp);

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"))
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        JsonNode verifyNode = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String resetToken = verifyNode.path("data").path("resetToken").asText();
        assertNotNull(resetToken);
        assertFalse(resetToken.isBlank());

        // 3. Reset Password
        String newPassword = "BrandNewPassword123!";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder()
                .resetToken(resetToken)
                .newPassword(newPassword)
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 4. Token Reuse Rejected
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));

        // 5. Old password no longer works
        LoginRequest oldLoginRequest = new LoginRequest();
        oldLoginRequest.setPhoneNumber(phoneNumber);
        oldLoginRequest.setPassword(initialPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());

        // 6. New password works and returns Access JWT + Refresh Token
        LoginRequest newLoginRequest = new LoginRequest();
        newLoginRequest.setPhoneNumber(phoneNumber);
        newLoginRequest.setPassword(newPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phoneNumber").value(phoneNumber));
    }

    @Test
    @DisplayName("Verify reset OTP with invalid OTP is rejected")
    void verifyResetOtp_InvalidOtp_Rejected() throws Exception {
        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp("000000");

        mockMvc.perform(post("/api/auth/verify-reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    @Test
    @DisplayName("Reset password with expired reset token is rejected")
    void resetPassword_ExpiredToken_Rejected() throws Exception {
        String rawToken = "expired-token-sample";
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .user(testUser)
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(2))
                .used(false)
                .build();
        passwordResetTokenRepository.save(expiredToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawToken)
                .newPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    @DisplayName("Reset password with invalid token is rejected")
    void resetPassword_InvalidToken_Rejected() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("non-existent-token")
                .newPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }
}
