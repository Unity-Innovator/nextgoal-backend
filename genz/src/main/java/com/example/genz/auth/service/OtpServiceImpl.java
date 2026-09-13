package com.example.genz.auth.service;

import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.SendPhoneOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.dto.VerifyOtpResponse;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final RedisOtpStore otpStore;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OtpHasher otpHasher;
    private final PlasGateSmsSender smsSender;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendOtp(SendOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();
        User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
        validateAccount(user);
        OtpPurpose purpose = request.getPurpose();
        if (purpose == null) {
            purpose = user == null || "PENDING".equalsIgnoreCase(user.getStatus())
                    ? OtpPurpose.REGISTRATION : OtpPurpose.PASSWORD_RESET;
        }
        if (purpose == OtpPurpose.PASSWORD_RESET && user == null) {
            throw new OtpException("No account found. Please register first.");
        }
        if (purpose == OtpPurpose.LOGIN && user == null) {
            throw new OtpException("No account found. Please register first.");
        }
        if (purpose == OtpPurpose.LOGIN && (user == null || !user.isEnabled())) {
            throw new OtpException("Account is not active");
        }
        String otp = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        String hash = otpHasher.hash(phoneNumber, purpose, otp);
        otpStore.save(phoneNumber, purpose, hash);
        try {
            smsSender.sendOtp(phoneNumber, otp);
        } catch (RuntimeException ex) {
            otpStore.removeIfMatching(phoneNumber, hash);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void sendPhoneOtp(SendPhoneOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();

        checkRateLimit(phoneNumber);
        invalidatePreviousOtps(phoneNumber);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        OtpPurpose purpose = userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> PENDING_STATUS.equalsIgnoreCase(user.getStatus()) ? OtpPurpose.REGISTRATION
                        : OtpPurpose.PASSWORD_RESET)
                .orElse(OtpPurpose.REGISTRATION);

        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        // Deliver OTP number directly to console for now
        deliverOtpToConsole(phoneNumber, otp, purpose);
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        if (request.getPurpose() == null) {
            throw new OtpException("Purpose is required");
        }
        String phoneNumber = request.getPhoneNumber();
        User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
        validateAccount(user);
        if (request.getPurpose() == OtpPurpose.PASSWORD_RESET && user == null) {
            throw new OtpException("No account found. Please register first.");
        }
        otpStore.consume(phoneNumber, request.getPurpose(),
                otpHasher.hash(phoneNumber, request.getPurpose(), request.getOtp()));

        if (request.getPurpose() == OtpPurpose.LOGIN) {
            if (user == null || !user.isEnabled()) {
                throw new OtpException("Account is not active");
            }
            UserResponse userResponse = userMapper.toUserResponse(user);
            return VerifyOtpResponse.builder()
                    .accessToken(jwtUtil.generateAccessToken(phoneNumber))
                    .refreshToken(jwtUtil.generateRefreshToken(phoneNumber))
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getAccessExpirationSeconds())
                    .user(userResponse)
                    .build();
        }

        // Invalidate previous active verification tokens for this phone number
        List<VerificationToken> activeTokens = verificationTokenRepository.findByPhoneNumberAndUsedFalse(phoneNumber);
        for (VerificationToken activeToken : activeTokens) {
            activeToken.setUsed(true);
            verificationTokenRepository.save(activeToken);
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String verificationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = TokenHashUtil.sha256Hex(verificationToken);

        VerificationToken tokenEntity = VerificationToken.builder()
                .phoneNumber(phoneNumber)
                .purpose(request.getPurpose())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        verificationTokenRepository.save(tokenEntity);

        return VerifyOtpResponse.builder()
                .verificationToken(verificationToken)
                .build();
    }

    private void validateAccount(User user) {
        if (user != null && !user.isEnabled() && !"PENDING".equalsIgnoreCase(user.getStatus())) {
            throw new OtpException("Account is not active");
        }

        if (purpose == OtpPurpose.PASSWORD_RESET && user == null) {
            throw new OtpException("No account found. Please register first.");
        }

        return user;
    }

    private void createAndSendOtp(User user, OtpPurpose purpose) {
        String phoneNumber = user.getPhoneNumber();
        checkRateLimit(phoneNumber);
        invalidatePreviousOtps(phoneNumber);

        String otp = generateOtp();
        Otp otpEntity = Otp.builder()
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash(hashOtp(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .attempts(0)
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        // Deliver to console
        deliverOtp(phoneNumber, otp, purpose);

        // Deliver to email
        if (user.getEmail() != null && !user.getEmail().isBlank() && emailOtpSender != null) {
            try {
                emailOtpSender.sendOtp(
                        user.getEmail(),
                        otp,
                        purpose,
                        otpProperties.getExpirationMinutes());
            } catch (Exception e) {
                log.warn("Failed to send email OTP to {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    private void deliverOtp(String phoneNumber, String otp, OtpPurpose purpose) {
        if (otpProperties.isConsoleDeliveryEnabled()) {
            log.warn("Development OTP for phone ending in {}: {}", maskedPhoneSuffix(phoneNumber), otp);
        }
    }

    private void deliverOtpToConsole(String phoneNumber, String otp, OtpPurpose purpose) {
        log.info("========================================");
        log.info("[OTP CONSOLE DELIVERY] Phone: {}, OTP: {}, Purpose: {}", phoneNumber, otp, purpose);
        log.info("========================================");
        System.out.println("[OTP CONSOLE DELIVERY] Phone: " + phoneNumber + " | OTP: " + otp + " | Purpose: " + purpose);
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
        StringBuilder otp = new StringBuilder(otpProperties.getLength());
        for (int i = 0; i < otpProperties.getLength(); i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private String hashOtp(String otp) {
        return TokenHashUtil.sha256Hex(otp);
    }

    private String maskedPhoneSuffix(String phoneNumber) {
        int suffixStart = Math.max(0, phoneNumber.length() - 4);
        return phoneNumber.substring(suffixStart);
    }
}
