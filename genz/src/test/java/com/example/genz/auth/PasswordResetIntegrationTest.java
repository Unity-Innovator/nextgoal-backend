package com.example.genz.auth;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.SendPhoneOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
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
    private VerificationTokenRepository verificationTokenRepository;

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
                .email("reset_user@example.com")
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
    @DisplayName("OTP verification with REGISTRATION purpose returns short-lived verification token")
    void otpVerification_ReturnsVerificationToken() throws Exception {
        String rawOtp = "654321";
        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .otpHash(TokenHashUtil.sha256Hex(rawOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp(rawOtp);
        verifyOtpRequest.setPurpose(OtpPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"))
                .andExpect(jsonPath("$.data.verificationToken").isNotEmpty());
    }

    @Test
    @DisplayName("Complete flow: send OTP -> verify OTP with PASSWORD_RESET -> reset password -> login with new password")
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
        otpEntity.setOtpHash(TokenHashUtil.sha256Hex(rawOtp));
        otpRepository.save(otpEntity);

        // 2. Verify OTP with PASSWORD_RESET purpose
        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp(rawOtp);
        verifyOtpRequest.setPurpose(OtpPurpose.PASSWORD_RESET);

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"))
                .andExpect(jsonPath("$.data.verificationToken").isNotEmpty())
                .andExpect(jsonPath("$.data.resetToken").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        JsonNode verifyNode = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String resetToken = verifyNode.path("data").path("verificationToken").asText();
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
    @DisplayName("Complete registration flow: send OTP -> verify OTP with REGISTRATION -> register with token -> login")
    void fullRegistrationFlow_Success() throws Exception {
        String newPhoneNumber = "0899887766";
        String email = "new_user@example.com";
        String password = "StrongPassword123!";

        // 1. Send OTP
        SendOtpRequest sendOtpRequest = new SendOtpRequest();
        sendOtpRequest.setPhoneNumber(newPhoneNumber);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String rawOtp = "654321";
        Otp otpEntity = otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(newPhoneNumber)
                .orElseThrow();
        otpEntity.setOtpHash(TokenHashUtil.sha256Hex(rawOtp));
        otpRepository.save(otpEntity);

        // 2. Verify OTP with REGISTRATION purpose
        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(newPhoneNumber);
        verifyOtpRequest.setOtp(rawOtp);
        verifyOtpRequest.setPurpose(OtpPurpose.REGISTRATION);

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationToken").isNotEmpty())
                .andReturn();

        JsonNode verifyNode = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String verificationToken = verifyNode.path("data").path("verificationToken").asText();

        // 3. Register with verification token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("New");
        registerRequest.setLastName("Member");
        registerRequest.setPhoneNumber(newPhoneNumber);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setConfirmPassword(password);
        registerRequest.setGender(Gender.MALE);
        registerRequest.setDateOfBirth(LocalDate.of(2001, 5, 20));
        registerRequest.setPin("654321");
        registerRequest.setVerificationToken(verificationToken);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phoneNumber").value(newPhoneNumber));

        // 4. Token Reuse Rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        // 5. Login immediately works
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber(newPhoneNumber);
        loginRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Security: Registration token cannot be used to reset password")
    void registrationProof_CannotResetPassword() throws Exception {
        String rawToken = "reg-only-token";
        VerificationToken regToken = VerificationToken.builder()
                .phoneNumber(phoneNumber)
                .user(testUser)
                .purpose(OtpPurpose.REGISTRATION)
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        verificationTokenRepository.save(regToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawToken)
                .newPassword("HackedPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification token purpose"));
    }

    @Test
    @DisplayName("Security: Password reset token cannot be used to register an account")
    void passwordResetProof_CannotRegisterAccount() throws Exception {
        String rawToken = "pwd-reset-only-token";
        String unusedPhone = "0977665544";
        VerificationToken resetToken = VerificationToken.builder()
                .phoneNumber(unusedPhone)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        verificationTokenRepository.save(resetToken);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Hacker");
        registerRequest.setLastName("User");
        registerRequest.setPhoneNumber(unusedPhone);
        registerRequest.setEmail("hacker@example.com");
        registerRequest.setPassword("SecretPass123!");
        registerRequest.setConfirmPassword("SecretPass123!");
        registerRequest.setGender(Gender.OTHER);
        registerRequest.setDateOfBirth(LocalDate.of(1995, 1, 1));
        registerRequest.setPin("123456");
        registerRequest.setVerificationToken(rawToken);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification token purpose"));
    }

    @Test
    @DisplayName("Verify OTP with invalid OTP is rejected")
    void verifyOtp_InvalidOtp_Rejected() throws Exception {
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
        verifyOtpRequest.setPurpose(OtpPurpose.REGISTRATION);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    @Test
    @DisplayName("Verify OTP with missing purpose is rejected")
    void verifyOtp_MissingPurpose_Rejected() throws Exception {
        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setPhoneNumber(phoneNumber);
        verifyOtpRequest.setOtp("123456");
        verifyOtpRequest.setPurpose(null);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reset password with expired reset token is rejected")
    void resetPassword_ExpiredToken_Rejected() throws Exception {
        String rawToken = "expired-token-sample";
        VerificationToken expiredToken = VerificationToken.builder()
                .phoneNumber(phoneNumber)
                .user(testUser)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(2))
                .used(false)
                .build();
        verificationTokenRepository.save(expiredToken);

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

    @Test
    @DisplayName("Send phone OTP endpoint accepts phone-only request and saves OTP")
    void sendPhoneOtp_Success() throws Exception {
        SendPhoneOtpRequest request = new SendPhoneOtpRequest("0998877665");

        mockMvc.perform(post("/api/auth/send-phone-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        assertTrue(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc("0998877665").isPresent());
    }

    @Test
    @DisplayName("Send phone OTP endpoint with blank phone number is rejected")
    void sendPhoneOtp_BlankPhone_Rejected() throws Exception {
        SendPhoneOtpRequest request = new SendPhoneOtpRequest("");

        mockMvc.perform(post("/api/auth/send-phone-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
