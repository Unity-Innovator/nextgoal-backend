package com.example.genz.auth.service;

import com.example.genz.auth.dto.StepUpTokenResponse;
import com.example.genz.auth.dto.VerifyPinRequest;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

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
}

