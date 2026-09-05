package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceImplTest {

    private OtpRepository otpRepository;
    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private EmailOtpSender emailOtpSender;
    private OtpServiceImpl service;

    @BeforeEach
    void setUp() {
        otpRepository = mock(OtpRepository.class);
        userRepository = mock(UserRepository.class);
        jwtUtil = mock(JwtUtil.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        emailOtpSender = mock(EmailOtpSender.class);

        OtpProperties properties = new OtpProperties();
        service = new OtpServiceImpl(
                otpRepository,
                userRepository,
                properties,
                jwtUtil,
                passwordEncoder,
                userMapper,
                emailOtpSender
        );
    }

    @Test
    void registrationOtpIsStoredAgainstPhoneAndDeliveredToEmail() {
        User user = User.builder()
                .phoneNumber("0123456789")
                .email("person@example.com")
                .status("PENDING")
                .build();
        when(userRepository.findByPhoneNumber(user.getPhoneNumber())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        service.sendRegistrationOtp(user.getPhoneNumber());

        ArgumentCaptor<Otp> storedOtp = ArgumentCaptor.forClass(Otp.class);
        ArgumentCaptor<String> deliveredOtp = ArgumentCaptor.forClass(String.class);
        verify(otpRepository).save(storedOtp.capture());
        verify(emailOtpSender).sendOtp(
                org.mockito.ArgumentMatchers.eq(user.getEmail()),
                deliveredOtp.capture(),
                org.mockito.ArgumentMatchers.eq(OtpPurpose.REGISTRATION),
                org.mockito.ArgumentMatchers.eq(5)
        );

        assertEquals(user.getPhoneNumber(), storedOtp.getValue().getPhoneNumber());
        assertEquals(OtpPurpose.REGISTRATION, storedOtp.getValue().getPurpose());
        assertEquals("hashed-otp", storedOtp.getValue().getOtpHash());
        assertFalse(storedOtp.getValue().isUsed());
        assertTrue(deliveredOtp.getValue().matches("\\d{6}"));
        verify(passwordEncoder).encode(deliveredOtp.getValue());
    }

    @Test
    void validRegistrationOtpActivatesUserAndReturnsTokens() {
        User user = User.builder()
                .phoneNumber("0123456789")
                .email("person@example.com")
                .status("PENDING")
                .build();
        Otp otp = validOtp(user.getPhoneNumber(), OtpPurpose.REGISTRATION);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(user.getPhoneNumber());
        request.setOtp("123456");
        UserResponse userResponse = UserResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .build();
        RegisterResponse expected = new RegisterResponse("access", "refresh", "Bearer", 900L, userResponse);

        when(userRepository.findByPhoneNumber(user.getPhoneNumber())).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                user.getPhoneNumber(),
                OtpPurpose.REGISTRATION
        )).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", otp.getOtpHash())).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getPhoneNumber())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(user.getPhoneNumber())).thenReturn("refresh");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);
        when(userMapper.toRegisterResponse("access", "refresh", 900L, userResponse)).thenReturn(expected);

        RegisterResponse result = service.verifyRegistrationOtp(request);

        assertEquals(expected, result);
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(otp.isUsed());
        verify(userRepository).save(user);
    }

    @Test
    void passwordResetOtpChangesPasswordButDoesNotActivateAnAccount() {
        User user = User.builder()
                .phoneNumber("0123456789")
                .email("person@example.com")
                .status("ACTIVE")
                .passwordHash("old-hash")
                .build();
        Otp otp = validOtp(user.getPhoneNumber(), OtpPurpose.PASSWORD_RESET);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhoneNumber(user.getPhoneNumber());
        request.setOtp("654321");
        request.setNewPassword("NewPassword1");
        request.setConfirmPassword("NewPassword1");

        when(userRepository.findByPhoneNumber(user.getPhoneNumber())).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                user.getPhoneNumber(),
                OtpPurpose.PASSWORD_RESET
        )).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("654321", otp.getOtpHash())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

        service.resetPassword(request);

        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(otp.isUsed());
        verify(userRepository).save(user);
    }

    @Test
    void forgotPasswordDoesNotRevealAnUnknownPhoneNumber() {
        when(userRepository.findByPhoneNumber("0000000000")).thenReturn(Optional.empty());

        service.sendPasswordResetOtp("0000000000");

        verify(otpRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(emailOtpSender, never()).sendOtp(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    private Otp validOtp(String phoneNumber, OtpPurpose purpose) {
        return Otp.builder()
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash("hashed-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .build();
    }
}
