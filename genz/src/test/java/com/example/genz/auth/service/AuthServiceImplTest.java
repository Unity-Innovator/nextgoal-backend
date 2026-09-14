package com.example.genz.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RefreshTokenRequest;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.StepUpTokenResponse;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.dto.VerifyPinRequest;
import com.example.genz.auth.entity.RefreshToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.RefreshTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.UnauthorizedException;
import com.example.genz.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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
    void register_Success_ValidatesVerificationTokenAndActivatesUser() {
        RegisterRequest request = registrationRequest();
        String rawToken = request.getVerificationToken();
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber(request.getPhoneNumber())
                .purpose(OtpPurpose.REGISTRATION)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        User mappedUser = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .status("ACTIVE")
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(verificationToken));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("password-hash");
        when(passwordEncoder.encode(request.getPin())).thenReturn("pin-hash");
        when(userMapper.toEntity(
                request,
                "password-hash",
                "pin-hash",
                "Test Person"
        )).thenReturn(mappedUser);
        when(userRepository.save(any(User.class))).thenReturn(mappedUser);
        when(jwtUtil.generateAccessToken(request.getPhoneNumber())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(request.getPhoneNumber())).thenReturn("refresh-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);
        UserResponse userResponse = UserResponse.builder().phoneNumber(request.getPhoneNumber()).build();
        RegisterResponse expectedResponse = RegisterResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900L)
                .user(userResponse)
                .build();
        when(userMapper.toUserResponse(mappedUser)).thenReturn(userResponse);
        when(userMapper.toRegisterResponse("access-token", "refresh-token", 900L, userResponse)).thenReturn(expectedResponse);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(expectedResponse, response);
        assertEquals("ACTIVE", mappedUser.getStatus());
        assertTrue(verificationToken.isUsed());
        assertEquals(mappedUser, verificationToken.getUser());

        verify(userRepository).existsByPhoneNumber(request.getPhoneNumber());
        verify(verificationTokenRepository).findByTokenHash(tokenHash);
        verify(verificationTokenRepository).save(verificationToken);
        verify(userRepository).save(mappedUser);
        verify(jwtUtil).generateAccessToken(request.getPhoneNumber());
        verify(jwtUtil).generateRefreshToken(request.getPhoneNumber());
    }

    @Test
    void register_WrongPurposeToken_ThrowsBadRequestException() {
        RegisterRequest request = registrationRequest();
        String tokenHash = TokenHashUtil.sha256Hex(request.getVerificationToken());

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber(request.getPhoneNumber())
                .purpose(OtpPurpose.PASSWORD_RESET)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(verificationToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertEquals("Invalid verification token purpose", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ExpiredVerificationToken_ThrowsBadRequestException() {
        RegisterRequest request = registrationRequest();
        String tokenHash = TokenHashUtil.sha256Hex(request.getVerificationToken());

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber(request.getPhoneNumber())
                .purpose(OtpPurpose.REGISTRATION)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(verificationToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertEquals("Invalid or expired verification token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_MismatchedPhoneNumber_ThrowsBadRequestException() {
        RegisterRequest request = registrationRequest();
        String tokenHash = TokenHashUtil.sha256Hex(request.getVerificationToken());

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0999999999")
                .purpose(OtpPurpose.REGISTRATION)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(verificationToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertEquals("Verification token does not match phone number", ex.getMessage());
        verify(userRepository, never()).save(any());
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

        VerificationToken resetTokenEntity = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0123456789")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        Void result = authService.resetPassword(request);

        assertNull(result);
        assertTrue(resetTokenEntity.isUsed());
        assertEquals(encodedNewPassword, user.getPasswordHash());

        verify(userRepository).save(user);
        verify(verificationTokenRepository).save(resetTokenEntity);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void resetPassword_WrongPurposeToken_ThrowsBadRequestException() {
        String rawResetToken = "test-reg-token-value-12345";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword("NewSecurePassword123!")
                .build();

        VerificationToken resetTokenEntity = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0123456789")
                .purpose(OtpPurpose.REGISTRATION)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Invalid verification token purpose", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsBadRequestException() {
        String rawResetToken = "test-expired-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawResetToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken(rawResetToken)
                .newPassword("NewPassword123!")
                .build();

        VerificationToken resetTokenEntity = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0123456789")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));

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

        VerificationToken resetTokenEntity = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber("0123456789")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(true)
                .build();

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetTokenEntity));

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

        when(verificationTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Invalid or expired reset token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyPin_Success() {
        String phoneNumber = "0123456789";
        String rawPin = "123456";
        String encodedPin = "encoded_123456";
        String stepUpToken = "mocked-step-up-jwt";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .status("ACTIVE")
                .pinHash(encodedPin)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(true);
        when(jwtUtil.generateStepUpToken(phoneNumber, JwtUtil.FINANCIAL_TRANSACTION_SCOPE)).thenReturn(stepUpToken);
        when(jwtUtil.getStepUpExpirationSeconds()).thenReturn(300L);

        StepUpTokenResponse response = authService.verifyPin(request, phoneNumber);

        assertNotNull(response);
        assertEquals(stepUpToken, response.stepUpToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Step-Up", response.tokenType());

        verify(pinAttemptService).checkAttempts(phoneNumber);
        verify(pinAttemptService).resetAttempts(phoneNumber);
        verify(pinAttemptService, never()).recordFailedAttempt(anyString());
    }

    @Test
    void verifyPin_InvalidPin_RecordsAttemptAndThrowsException() {
        String phoneNumber = "0123456789";
        String rawPin = "000000";
        String encodedPin = "encoded_123456";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .status("ACTIVE")
                .pinHash(encodedPin)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("Invalid PIN", exception.getMessage());

        verify(pinAttemptService).checkAttempts(phoneNumber);
        verify(pinAttemptService).recordFailedAttempt(phoneNumber);
        verify(pinAttemptService, never()).resetAttempts(anyString());
    }

    @Test
    void verifyPin_UserHasNoPin_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .status("ACTIVE")
                .pinHash(null)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("User does not have a PIN configured", exception.getMessage());

        verify(pinAttemptService).checkAttempts(phoneNumber);
        verify(pinAttemptService, never()).recordFailedAttempt(anyString());
        verify(pinAttemptService, never()).resetAttempts(anyString());
    }

    @Test
    void verifyPin_UserDisabled_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .pin("123456")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .status("SUSPENDED")
                .pinHash("encoded_123456")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request, phoneNumber));
        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    void verifyPin_TooManyAttempts_ThrowsBadRequestException() {
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

    @Test
    void refresh_Success_RotatesTokensAndRevokesPreviousToken() {
        String phoneNumber = "0123456789";
        String rawToken = "valid-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(phoneNumber);
        when(jwtUtil.extractAndValidateRefreshToken(rawToken)).thenReturn(claims);

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .status("ACTIVE")
                .build();
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));

        when(jwtUtil.generateAccessToken(phoneNumber)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(phoneNumber)).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(604800000L);
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);

        UserResponse userResponse = UserResponse.builder()
                .phoneNumber(phoneNumber)
                .build();
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
        when(userMapper.toLoginResponse("new-access-token", "new-refresh-token", 900L, userResponse))
                .thenReturn(expectedResponse);

        LoginResponse result = authService.refresh(new RefreshTokenRequest(rawToken));

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
        assertTrue(storedToken.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_InvalidJwt_ThrowsUnauthorizedException() {
        String rawToken = "malformed-or-expired-jwt";
        when(jwtUtil.extractAndValidateRefreshToken(rawToken)).thenThrow(new JwtException("Invalid token"));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(new RefreshTokenRequest(rawToken)));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refresh_ReusedRevokedToken_TriggersRevokeAllForUserAndThrowsUnauthorized() {
        String phoneNumber = "0123456789";
        String rawToken = "stolen-already-used-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(phoneNumber);
        when(jwtUtil.extractAndValidateRefreshToken(rawToken)).thenReturn(claims);

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .status("ACTIVE")
                .build();
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        RefreshToken revokedStoredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedStoredToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.refresh(new RefreshTokenRequest(rawToken)));
        assertTrue(ex.getMessage().contains("revoked"));
        verify(refreshTokenRepository).revokeAllForUser(user);
        verify(jwtUtil, never()).generateAccessToken(anyString());
    }

    @Test
    void logout_Success_MarksTokenRevoked() {
        String rawToken = "active-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));

        authService.logout(new RefreshTokenRequest(rawToken));

        assertTrue(storedToken.isRevoked());
        verify(refreshTokenRepository).save(storedToken);
    }

    private RegisterRequest registrationRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("Person");
        request.setPhoneNumber("0123456789");
        request.setPassword("Password1");
        request.setConfirmPassword("Password1");
        request.setGender(Gender.OTHER);
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setPin("123456");
        request.setVerificationToken("sample-valid-verification-token");
        return request;
    }
}
