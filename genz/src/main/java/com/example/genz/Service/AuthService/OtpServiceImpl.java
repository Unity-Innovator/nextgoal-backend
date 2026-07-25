package com.example.genz.Service.AuthService;

import com.example.genz.config.OtpProperties;
import com.example.genz.DTO.AuthDTO.LoginResponse;
import com.example.genz.DTO.AuthDTO.SendOtpRequest;
import com.example.genz.DTO.AuthDTO.UserResponse;
import com.example.genz.DTO.AuthDTO.VerifyOtpRequest;
import com.example.genz.Entity.OTP.Otp;
import com.example.genz.Entity.OTP.User;
import com.example.genz.Repository.AuthRepository.OtpRepository;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Exception.OtpException;
import com.example.genz.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final OtpProperties otpProperties;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();

        checkRateLimit(phoneNumber);

        invalidatePreviousOtps(phoneNumber);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        logOtp(phoneNumber, otp);
    }

    @Override
    @Transactional
    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();
        String otp = request.getOtp();

        Otp otpEntity = otpRepository
                .findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> new OtpException("No OTP found for this phone number"));

        if (otpEntity.isUsed()) {
            throw new OtpException("OTP has already been used");
        }

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

        if (!otpEntity.getOtpHash().equals(hashOtp(otp))) {
            otpRepository.save(otpEntity);
            throw new OtpException("Invalid OTP");
        }

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new OtpException("No account found. Please register first."));

        String accessToken = jwtUtil.generateAccessToken(phoneNumber);
        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
    }

    private void checkRateLimit(String phoneNumber) {
        LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(otpProperties.getResendCooldownSeconds());
        long recentOtps = otpRepository.countByPhoneNumberAndCreatedAtAfter(phoneNumber, cooldownStart);
        if (recentOtps > 0) {
            throw new OtpException("Please wait before requesting a new OTP");
        }
    }

    private void invalidatePreviousOtps(String phoneNumber) {
        otpRepository.findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    otpRepository.save(existing);
                });
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpProperties.getLength(); i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void logOtp(String phoneNumber, String otp) {
        System.out.println("========================================");
        System.out.println("OTP for " + phoneNumber + ": " + otp);
        System.out.println("========================================");
    }

}
