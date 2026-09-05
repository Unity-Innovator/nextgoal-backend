package com.example.genz.auth.service;

import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.auth.dto.*;
import com.example.genz.auth.entity.User;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PinAttemptService pinAttemptService;
    private final OtpService otpService;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phoneNumber = user.getPhoneNumber();
        String jwt = jwtUtil.generateAccessToken(phoneNumber);
        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return userMapper.toLoginResponse(jwt, refreshToken, jwtUtil.getAccessExpirationSeconds(), userResponse);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        final String fullName = request.getFirstName() + " " + request.getLastName();
        final String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already exists");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email already exists");
        }

        request.setEmail(normalizedEmail);

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String pinHash = passwordEncoder.encode(request.getPin());

        User user = userMapper.toEntity(request, passwordHash, pinHash, fullName);
        user.setStatus("PENDING");
        userRepository.save(user);

        otpService.sendRegistrationOtp(user.getPhoneNumber());
    }

    @Override
    public Boolean checkUser(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
        return user != null;
    }

    @Override
    public StepUpTokenResponse verifyPin(VerifyPinRequest request, String phoneNumber) {
        pinAttemptService.checkAttempts(phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("User not found"));

        ensureActive(user);

        if (user.getPinHash() == null) {
            throw new BadRequestException("User does not have a PIN configured");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getPinHash())) {
            pinAttemptService.recordFailedAttempt(phoneNumber);
            throw new BadRequestException("Invalid PIN");
        }

        pinAttemptService.resetAttempts(phoneNumber);

        String stepUpToken = jwtUtil.generateStepUpToken(phoneNumber, JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        return StepUpTokenResponse.builder()
                .stepUpToken(stepUpToken)
                .expiresIn(jwtUtil.getStepUpExpirationSeconds())
                .tokenType("Step-Up")
                .build();
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        try {
            String phoneNumber = jwtUtil.extractPhoneNumber(request.refreshToken());
            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
            if (!jwtUtil.validateRefreshToken(request.refreshToken(), user)) {
                throw new BadRequestException("Invalid refresh token");
            }

            String accessToken = jwtUtil.generateAccessToken(phoneNumber);
            String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);
            return userMapper.toLoginResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getAccessExpirationSeconds(),
                    userMapper.toUserResponse(user)
            );
        } catch (RuntimeException ex) {
            if (ex instanceof BadRequestException badRequestException) {
                throw badRequestException;
            }
            throw new BadRequestException("Invalid refresh token");
        }
    }

    private void ensureActive(UserDetails user) {
        if (!user.isEnabled()) {
            throw new BadRequestException("Account is not active");
        }
    }
}
