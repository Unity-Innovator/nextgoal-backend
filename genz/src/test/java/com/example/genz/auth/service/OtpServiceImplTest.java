package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.SendPhoneOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.dto.VerifyOtpResponse;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
import com.example.genz.auth.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailOtpSender emailOtpSender;

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
                verificationTokenRepository,
                properties,
                passwordEncoder,
                emailOtpSender
        );
    }

    @Test
    void productionSafeDefaultDoesNotPersistOrLogAnUndeliverableOtp() {
        properties.setConsoleDeliveryEnabled(false);
        service = new OtpServiceImpl(
                otpRepository,
                userRepository,
                verificationTokenRepository,
                properties,
                passwordEncoder,
                null
        );
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber("0123456789");

        OtpException exception = assertThrows(OtpException.class, () -> service.sendOtp(request));

        assertEquals("OTP delivery is not configured", exception.getMessage());
        verifyNoInteractions(otpRepository, userRepository, verificationTokenRepository);
    }

    @Test
    void verifyOtp_MissingPurpose_ThrowsOtpException() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("0123456789");
        request.setOtp("123456");
        request.setPurpose(null);

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyOtp(request));
        assertEquals("Purpose is required", exception.getMessage());
    }

    @Test
    void verifyOtp_Registration_Success_GeneratesRegistrationVerificationToken() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";
        String otpHash = TokenHashUtil.sha256Hex(rawOtp);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);
        request.setPurpose(OtpPurpose.REGISTRATION);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(verificationTokenRepository.findByPhoneNumberAndUsedFalse(phoneNumber)).thenReturn(List.of());

        VerifyOtpResponse response = service.verifyOtp(request);

        assertNotNull(response);
        assertNotNull(response.getVerificationToken());
        assertFalse(response.getVerificationToken().isBlank());
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(phoneNumber, savedToken.getPhoneNumber());
        assertEquals(OtpPurpose.REGISTRATION, savedToken.getPurpose());
        assertNull(savedToken.getUser());
        assertEquals(TokenHashUtil.sha256Hex(response.getVerificationToken()), savedToken.getTokenHash());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void verifyOtp_PasswordReset_Success_ReturnsPasswordResetTokenAndInvalidatesPreviousTokens() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";
        String otpHash = TokenHashUtil.sha256Hex(rawOtp);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);
        request.setPurpose(OtpPurpose.PASSWORD_RESET);

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

        VerificationToken previousToken = VerificationToken.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByPhoneNumberAndUsedFalse(phoneNumber)).thenReturn(List.of(previousToken));

        VerifyOtpResponse response = service.verifyOtp(request);

        assertNotNull(response);
        assertNotNull(response.getVerificationToken());

        // OTP should be marked as used
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);

        // Previous token should be invalidated
        assertTrue(previousToken.isUsed());
        verify(verificationTokenRepository).save(previousToken);

        // New token saved
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository, times(2)).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getAllValues().get(tokenCaptor.getAllValues().size() - 1);
        assertEquals(user, savedToken.getUser());
        assertEquals(phoneNumber, savedToken.getPhoneNumber());
        assertEquals(OtpPurpose.PASSWORD_RESET, savedToken.getPurpose());
        assertEquals(TokenHashUtil.sha256Hex(response.getVerificationToken()), savedToken.getTokenHash());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void verifyOtp_PasswordReset_UserNotFound_ThrowsOtpException() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);
        request.setPurpose(OtpPurpose.PASSWORD_RESET);

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

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyOtp(request));
        assertEquals("No account found. Please register first.", exception.getMessage());
    }

    @Test
    void verifyOtp_InvalidOtp_ThrowsOtpExceptionAndIncrementsAttempts() {
        String phoneNumber = "0123456789";
        String correctOtp = "123456";
        String enteredOtp = "654321";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(enteredOtp);
        request.setPurpose(OtpPurpose.REGISTRATION);

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

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyOtp(request));
        assertEquals("Invalid OTP", exception.getMessage());
        assertEquals(1, otpEntity.getAttempts());
        assertFalse(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
        verifyNoInteractions(verificationTokenRepository, userRepository);
    }

    @Test
    void verifyOtp_ExpiredOtp_ThrowsOtpExceptionAndMarksUsed() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);
        request.setPurpose(OtpPurpose.REGISTRATION);

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

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyOtp(request));
        assertEquals("OTP has expired", exception.getMessage());
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
        verifyNoInteractions(verificationTokenRepository);
    }

    @Test
    void verifyOtp_InactiveUser_ThrowsOtpException() {
        String phoneNumber = "0123456789";
        String rawOtp = "123456";

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phoneNumber);
        request.setOtp(rawOtp);
        request.setPurpose(OtpPurpose.PASSWORD_RESET);

        Otp otpEntity = Otp.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .otpHash(TokenHashUtil.sha256Hex(rawOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();

        User disabledUser = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .status("SUSPENDED")
                .build();

        when(otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(otpEntity));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(disabledUser));

        OtpException exception = assertThrows(OtpException.class, () -> service.verifyOtp(request));
        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    void sendPhoneOtp_Success_SavesOtpAndDoesNotTriggerEmailSender() {
        String phoneNumber = "0123456789";
        SendPhoneOtpRequest request = new SendPhoneOtpRequest(phoneNumber);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(otpRepository.countByPhoneNumberAndCreatedAtAfter(eq(phoneNumber), any())).thenReturn(0L);

        service.sendPhoneOtp(request);

        ArgumentCaptor<Otp> otpCaptor = ArgumentCaptor.forClass(Otp.class);
        verify(otpRepository).save(otpCaptor.capture());
        Otp savedOtp = otpCaptor.getValue();

        assertEquals(phoneNumber, savedOtp.getPhoneNumber());
        assertEquals(OtpPurpose.REGISTRATION, savedOtp.getPurpose());
        assertNotNull(savedOtp.getOtpHash());
        assertFalse(savedOtp.isUsed());
        assertEquals(0, savedOtp.getAttempts());
        verifyNoInteractions(emailOtpSender);
    }

    @Test
    void sendPhoneOtp_RateLimitExceeded_ThrowsOtpException() {
        String phoneNumber = "0123456789";
        SendPhoneOtpRequest request = new SendPhoneOtpRequest(phoneNumber);

        when(otpRepository.countByPhoneNumberAndCreatedAtAfter(eq(phoneNumber), any())).thenReturn(1L);

        OtpException exception = assertThrows(OtpException.class, () -> service.sendPhoneOtp(request));
        assertEquals("Please wait before requesting a new OTP", exception.getMessage());
        verify(otpRepository, never()).save(any());
    }
}
