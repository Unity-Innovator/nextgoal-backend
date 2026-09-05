package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.StepUpTokenResponse;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.dto.VerifyPinRequest;
import com.example.genz.auth.entity.PasswordResetToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.PasswordResetTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PinAttemptService pinAttemptService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_Success_ReturnsAccessAndRefreshToken() {
        String phoneNumber = "0123456789";
        String password = "Password123!";
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .status("ACTIVE")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName("John Doe")
                .phoneNumber(phoneNumber)
                .build();

        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("access-token-jwt")
                .refreshToken("refresh-token-jwt")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(phoneNumber)).thenReturn("access-token-jwt");
        when(jwtUtil.generateRefreshToken(phoneNumber)).thenReturn("refresh-token-jwt");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);
        when(userMapper.toLoginResponse("access-token-jwt", "refresh-token-jwt", 900L, userResponse))
                .thenReturn(expectedResponse);

        LoginResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token-jwt", result.accessToken());
        assertEquals("refresh-token-jwt", result.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void registerCreatesPendingUserAndSendsEmailOtpUsingPhoneNumber() {
        RegisterRequest request = registrationRequest();
        User mappedUser = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .email("person@example.com")
                .status("ACTIVE")
                .build();

        when(passwordEncoder.encode(request.getPassword())).thenReturn("password-hash");
        when(passwordEncoder.encode(request.getPin())).thenReturn("pin-hash");
        when(userMapper.toEntity(
                request,
                "password-hash",
                "pin-hash",
                "Test Person"
        )).thenReturn(mappedUser);

        authService.register(request);

        assertEquals("person@example.com", request.getEmail());
        assertEquals("PENDING", mappedUser.getStatus());
        verify(userRepository).existsByPhoneNumber(request.getPhoneNumber());
        verify(userRepository).existsByEmailIgnoreCase("person@example.com");
        verify(userRepository).save(mappedUser);
        verify(otpService).sendRegistrationOtp(request.getPhoneNumber());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void resetPassword_Success_UpdatesPasswordAndMarksTokenUsed() {
        String rawResetToken = "test-raw-reset-token-value-12345";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);
        String newPassword = "NewSecurePassword123!";
        String encodedNewPassword = "bcrypt-encoded-new-password";

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword(newPassword)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0123456789")
                .passwordHash("old-password-hash")
                .status("ACTIVE")
                .build();

        PasswordResetToken resetTokenEntity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        Void result = authService.resetPassword(request);

        assertNull(result);
        assertTrue(resetTokenEntity.isUsed());
        assertEquals(encodedNewPassword, user.getPasswordHash());

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetTokenEntity);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsBadRequestException() {
        String rawResetToken = "test-expired-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword("NewPassword123!")
                .build();

        PasswordResetToken resetTokenEntity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Invalid or expired reset token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_AlreadyUsedToken_ThrowsBadRequestException() {
        String rawResetToken = "test-used-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword("NewPassword123!")
                .build();

        PasswordResetToken resetTokenEntity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Invalid or expired reset token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsBadRequestException() {
        String rawResetToken = "non-existent-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword("NewPassword123!")
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Invalid or expired reset token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyPin_Success() {
        String phoneNumber = "0123456789";
        String rawPin = "123456";
        String encodedPin = "encoded_123456";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash(encodedPin)
                .fullName("John Doe")
                .status("ACTIVE")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(true);
        when(jwtUtil.generateStepUpToken(phoneNumber, JwtUtil.FINANCIAL_TRANSACTION_SCOPE)).thenReturn("mock-step-up-token");
        when(jwtUtil.getStepUpExpirationSeconds()).thenReturn(300L);

        StepUpTokenResponse response = authService.verifyPin(request, phoneNumber);

        assertNotNull(response);
        assertEquals("mock-step-up-token", response.stepUpToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Step-Up", response.tokenType());

        verify(pinAttemptService).checkAttempts(phoneNumber);
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(passwordEncoder).matches(rawPin, encodedPin);
        verify(pinAttemptService).resetAttempts(phoneNumber);
    }

    @Test
    void verifyPin_InvalidPin_ThrowsBadRequestExceptionAndRecordsAttempt() {
        String phoneNumber = "0123456789";
        String rawPin = "123456";
        String encodedPin = "encoded_123456";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash(encodedPin)
                .status("ACTIVE")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("Invalid PIN", exception.getMessage());
        verify(pinAttemptService).recordFailedAttempt(phoneNumber);
        verify(pinAttemptService, never()).resetAttempts(anyString());
    }

    @Test
    void verifyPin_NoPinConfigured_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash(null)
                .status("ACTIVE")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("User does not have a PIN configured", exception.getMessage());
    }

    @Test
    void verifyPin_InactiveAccount_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash("encoded_pin")
                .status("INACTIVE")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    void verifyPin_LockedOut_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        doThrow(new BadRequestException("Too many failed PIN attempts. Please try again later."))
                .when(pinAttemptService).checkAttempts(phoneNumber);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertTrue(exception.getMessage().contains("Too many failed PIN attempts"));
    }

    @Test
    void verifyPin_UserNotFound_ThrowsBadRequestException() {
        String phoneNumber = "0000000000";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
    }

    private RegisterRequest registrationRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("Person");
        request.setPhoneNumber("0123456789");
        request.setEmail("Person@Example.com");
        request.setPassword("Password1");
        request.setConfirmPassword("Password1");
        request.setGender(Gender.OTHER);
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setPin("123456");
        return request;
    }
}
