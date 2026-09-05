package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.ResetTokenResponse;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.PasswordResetToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.PasswordResetTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private OtpProperties properties;
    private OtpServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new OtpProperties();
        properties.setConsoleDeliveryEnabled(true);
        properties.setExpirationMinutes(5);
        properties.setMaxAttempts(3);
        properties.setResendCooldownSeconds(60);
        properties.setLength(6);

        service = new OtpServiceImpl(
                otpRepository,
                userRepository,
                passwordResetTokenRepository,
                properties
        );
    }

    @Test
    void productionSafeDefaultDoesNotPersistOrLogAnUndeliverableOtp() {
        properties.setConsoleDeliveryEnabled(false);
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber("0123456789");

        OtpException exception = assertThrows(OtpException.class, () -> service.sendOtp(request));

        assertEquals("OTP delivery is not configured", exception.getMessage());
        verifyNoInteractions(otpRepository, userRepository, passwordResetTokenRepository);
    }

    @Test
    void verifyOtp_Registration_Success_DoesNotGenerateTokens() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";
        String otpHash = TokenHashUtil.sha256Hex(rawOtp);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("Test User")
                .status("ACTIVE")
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> service.verifyOtp(request));

        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
        verifyNoInteractions(passwordResetTokenRepository);
    }

    @Test
    void verifyResetOtp_Success_ReturnsResetTokenAndInvalidatesPreviousTokens() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";
        String otpHash = TokenHashUtil.sha256Hex(rawOtp);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .fullName("Test User")
                .status("ACTIVE")
                .build();

        PasswordResetToken previousToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndUsedFalse(user)).thenReturn(List.of(previousToken));

        ResetTokenResponse response = service.verifyResetOtp(request);

        assertNotNull(response);
        assertNotNull(response.getResetToken());
        assertFalse(response.getResetToken().isBlank());

        // OTP should be marked as used
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);

        // Previous reset token should be invalidated
        assertTrue(previousToken.isUsed());
        verify(passwordResetTokenRepository).save(previousToken);

        // New reset token saved
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository, times(2)).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getAllValues().get(tokenCaptor.getAllValues().size() - 1);
        assertEquals(user, savedToken.getUser());
        assertEquals(TokenHashUtil.sha256Hex(response.getResetToken()), savedToken.getTokenHash());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void verifyResetOtp_InvalidOtp_ThrowsOtpExceptionAndIncrementsAttempts() {
        String phoneNumber = "0123456789";
        String correctOtp = "123456";
        String enteredOtp = "654321";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(enteredOtp);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex(correctOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyResetOtp(request));
        assertEquals("Invalid OTP", exception.getMessage());
        assertEquals(1, otpEntity.getAttempts());
        assertFalse(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
        verifyNoInteractions(passwordResetTokenRepository, userRepository);
    }

    @Test
    void verifyResetOtp_ExpiredOtp_ThrowsOtpExceptionAndMarksUsed() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex(rawOtp))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attempts(0)
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyResetOtp(request));
        assertEquals("OTP has expired", exception.getMessage());
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
    }

    @Test
    void verifyResetOtp_UserNotFound_ThrowsOtpException() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex(rawOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyResetOtp(request));
        assertEquals("No account found. Please register first.", exception.getMessage());
    }
}
