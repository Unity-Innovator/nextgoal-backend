package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.dto.VerifyPinRequest;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
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

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void verifyPin_Success() {
        String phoneNumber = "0123456789";
        String rawPin = "123456";
        String encodedPin = "encoded_123456";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .phoneNumber(phoneNumber)
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash(encodedPin)
                .fullName("John Doe")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .phoneNumber(phoneNumber)
                .fullName("John Doe")
                .build();

        LoginResponse expectedLoginResponse = LoginResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(true);
        when(jwtUtil.generateAccessToken(phoneNumber)).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(phoneNumber)).thenReturn("mock-refresh-token");
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);
        when(userMapper.toLoginResponse("mock-access-token", "mock-refresh-token", userResponse))
                .thenReturn(expectedLoginResponse);

        LoginResponse response = authService.verifyPin(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(passwordEncoder).matches(rawPin, encodedPin);
    }

    @Test
    void verifyPin_InvalidPin_ThrowsBadRequestException() {
        String phoneNumber = "0123456789";
        String rawPin = "123456";
        String encodedPin = "encoded_123456";

        VerifyPinRequest request = VerifyPinRequest.builder()
                .phoneNumber(phoneNumber)
                .pin(rawPin)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .pinHash(encodedPin)
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPin, encodedPin)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.verifyPin(request));
        assertEquals("Invalid PIN", exception.getMessage());
    }

    @Test
    void verifyPin_UserNotFound_ThrowsResourceNoteFoundException() {
        String phoneNumber = "0000000000";
        VerifyPinRequest request = VerifyPinRequest.builder()
                .phoneNumber(phoneNumber)
                .pin("123456")
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        assertThrows(ResourceNoteFoundException.class, () -> authService.verifyPin(request));
    }
}
