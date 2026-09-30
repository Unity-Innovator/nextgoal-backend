package com.example.genz.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.RefreshTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.common.exception.UnauthorizedException;
import com.example.genz.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PinAttemptService pinAttemptService;
    private final com.example.genz.wallet.repository.WalletRepository walletRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phoneNumber = user.getPhoneNumber();
        String jwt = jwtUtil.generateAccessToken(phoneNumber);
        String refreshToken = createAndSaveRefreshToken(user);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return userMapper.toLoginResponse(jwt, refreshToken, jwtUtil.getAccessExpirationSeconds(), userResponse);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        final String fullName = request.getFirstName() + " " + request.getLastName();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already exists");
        }

        if (request.getVerificationToken() == null || request.getVerificationToken().isBlank()) {
            throw new BadRequestException("Verification token is required");
        }

        String rawToken = request.getVerificationToken().trim();
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification token");
        }

        if (verificationToken.getPurpose() != com.example.genz.auth.enums.OtpPurpose.REGISTRATION) {
            throw new BadRequestException("Invalid verification token purpose");
        }

        if (!request.getPhoneNumber().equals(verificationToken.getPhoneNumber())) {
            throw new BadRequestException("Verification token does not match phone number");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String pinHash = passwordEncoder.encode(request.getPin());

        User user = userMapper.toEntity(request, passwordHash, pinHash, fullName);
        user.setStatus("ACTIVE");
        User savedUser = userRepository.save(user);

        com.example.genz.wallet.entity.Wallet userWallet = com.example.genz.wallet.entity.Wallet.builder()
                .user(savedUser)
                .balance(java.math.BigDecimal.ZERO)
                .currency(com.example.genz.transaction.enums.Currency.USD)
                .build();
        walletRepository.save(userWallet);

        verificationToken.setUser(savedUser);
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        String phoneNumber = savedUser.getPhoneNumber();
        String jwt = jwtUtil.generateAccessToken(phoneNumber);
        String refreshToken = createAndSaveRefreshToken(savedUser);

        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return userMapper.toRegisterResponse(jwt, refreshToken, jwtUtil.getAccessExpirationSeconds(), userResponse);
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
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.refreshToken();
        Claims claims;
        try {
            claims = jwtUtil.extractAndValidateRefreshToken(rawToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String phoneNumber = claims.getSubject();
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is not active");
        }

        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            // Token reuse detected! Invalidate all active tokens for this user
            refreshTokenRepository.revokeAllForUser(user);
            throw new UnauthorizedException("Refresh token has been revoked. All active sessions invalidated.");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Mark current refresh token as revoked (single-use)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtUtil.generateAccessToken(phoneNumber);
        String newRefreshToken = createAndSaveRefreshToken(user);

        return userMapper.toLoginResponse(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessExpirationSeconds(),
                userMapper.toUserResponse(user)
        );
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = TokenHashUtil.sha256Hex(request.refreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Override
    @Transactional
    public Void resetPassword(ResetPasswordRequest request) {
        if (request.getResetToken() == null || request.getResetToken().isBlank()) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        String rawToken = request.getResetToken().trim();
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        VerificationToken resetToken = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        if (resetToken.getPurpose() != com.example.genz.auth.enums.OtpPurpose.PASSWORD_RESET) {
            throw new BadRequestException("Invalid verification token purpose");
        }

        User user = resetToken.getUser();
        if (user == null) {
            user = userRepository.findByPhoneNumber(resetToken.getPhoneNumber())
                    .orElseThrow(() -> new BadRequestException("Account not found"));
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is not active");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        // Revoke all active sessions on password reset
        refreshTokenRepository.revokeAllForUser(user);

        resetToken.setUsed(true);
        verificationTokenRepository.save(resetToken);

        return null;
    }

    private String createAndSaveRefreshToken(User user) {
        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());
        String tokenHash = TokenHashUtil.sha256Hex(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(
                Duration.ofMillis(jwtUtil.getRefreshExpirationMillis())
        );

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return refreshToken;
    }

    private void ensureActive(UserDetails user) {
        if (!user.isEnabled()) {
            throw new BadRequestException("Account is not active");
        }
    }
}
