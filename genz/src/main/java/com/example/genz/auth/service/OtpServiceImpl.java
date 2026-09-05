package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String PENDING_STATUS = "PENDING";

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final OtpProperties otpProperties;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailOtpSender emailOtpSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void sendRegistrationOtp(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new OtpException("Registration was not found"));

        if (!PENDING_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new OtpException("Registration is already verified");
        }

        createAndSendOtp(user, OtpPurpose.REGISTRATION);
    }

    @Override
    @Transactional
    public RegisterResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new OtpException("Invalid or expired OTP"));

        if (!PENDING_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new OtpException("Registration is already verified");
        }

        verifyAndConsumeOtp(request.getPhoneNumber(), request.getOtp(), OtpPurpose.REGISTRATION);

        user.setStatus("ACTIVE");
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getPhoneNumber());
        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());
        UserResponse userResponse = userMapper.toUserResponse(user);

        return userMapper.toRegisterResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessExpirationSeconds(),
                userResponse
        );
    }

    @Override
    @Transactional
    public void sendPasswordResetOtp(String phoneNumber) {
        userRepository.findByPhoneNumber(phoneNumber)
                .filter(User::isEnabled)
                .ifPresent(user -> createAndSendOtp(user, OtpPurpose.PASSWORD_RESET));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new OtpException("Passwords do not match");
        }

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .filter(User::isEnabled)
                .orElseThrow(() -> new OtpException("Invalid or expired OTP"));

        verifyAndConsumeOtp(request.getPhoneNumber(), request.getOtp(), OtpPurpose.PASSWORD_RESET);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void createAndSendOtp(User user, OtpPurpose purpose) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new OtpException("No email address is configured for this account");
        }

        String phoneNumber = user.getPhoneNumber();
        checkRateLimit(phoneNumber, purpose);
        invalidatePreviousOtps(phoneNumber, purpose);

        String otp = generateOtp();
        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        emailOtpSender.sendOtp(
                user.getEmail(),
                otp,
                purpose,
                otpProperties.getExpirationMinutes()
        );
    }

    private void verifyAndConsumeOtp(String phoneNumber, String otp, OtpPurpose purpose) {
        Otp otpEntity = otpRepository
                .findFirstByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(phoneNumber, purpose)
                .orElseThrow(() -> new OtpException("Invalid or expired OTP"));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new OtpException("OTP has expired");
        }

        if (otpEntity.getAttempts() >= otpProperties.getMaxAttempts()) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new OtpException("Maximum OTP attempts exceeded");
        }

        otpEntity.setAttempts(otpEntity.getAttempts() + 1);

        if (!passwordEncoder.matches(otp, otpEntity.getOtpHash())) {
            if (otpEntity.getAttempts() >= otpProperties.getMaxAttempts()) {
                otpEntity.setUsed(true);
            }
            otpRepository.save(otpEntity);
            throw new OtpException("Invalid OTP");
        }

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);
    }

    private void checkRateLimit(String phoneNumber, OtpPurpose purpose) {
        LocalDateTime cooldownStart = LocalDateTime.now()
                .minusSeconds(otpProperties.getResendCooldownSeconds());
        long recentOtps = otpRepository.countByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber,
                purpose,
                cooldownStart
        );
        if (recentOtps > 0) {
            throw new OtpException("Please wait before requesting a new OTP");
        }
    }

    private void invalidatePreviousOtps(String phoneNumber, OtpPurpose purpose) {
        otpRepository.findFirstByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(phoneNumber, purpose)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    otpRepository.save(existing);
                });
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder(otpProperties.getLength());
        for (int i = 0; i < otpProperties.getLength(); i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
