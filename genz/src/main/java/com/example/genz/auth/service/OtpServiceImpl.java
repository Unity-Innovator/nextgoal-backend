package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.ResetTokenResponse;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.entity.PasswordResetToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.PasswordResetTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final String PENDING_STATUS = "PENDING";

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpProperties otpProperties;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpSender emailOtpSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();
        if (!otpProperties.isConsoleDeliveryEnabled() && emailOtpSender == null) {
            throw new OtpException("OTP delivery is not configured");
        }

        checkRateLimit(phoneNumber);
        invalidatePreviousOtps(phoneNumber);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        OtpPurpose purpose = userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> PENDING_STATUS.equalsIgnoreCase(user.getStatus()) ? OtpPurpose.REGISTRATION : OtpPurpose.PASSWORD_RESET)
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

        // Deliver to console (your flow)
        deliverOtp(phoneNumber, otp, purpose);

        // Deliver to email (friend's flow)
        userRepository.findByPhoneNumber(phoneNumber)
                .ifPresent(user -> {
                    if (user.getEmail() != null && !user.getEmail().isBlank() && emailOtpSender != null) {
                        try {
                            emailOtpSender.sendOtp(
                                    user.getEmail(),
                                    otp,
                                    purpose,
                                    otpProperties.getExpirationMinutes()
                            );
                        } catch (Exception e) {
                            log.warn("Failed to send email OTP to {}: {}", user.getEmail(), e.getMessage());
                        }
                    }
                });
    }

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
    public void sendPasswordResetOtp(String phoneNumber) {
        userRepository.findByPhoneNumber(phoneNumber)
                .filter(User::isEnabled)
                .ifPresent(user -> createAndSendOtp(user, OtpPurpose.PASSWORD_RESET));
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = validateAndConsumeOtp(request.getPhoneNumber(), request.getOtp());
        if (PENDING_STATUS.equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public ResetTokenResponse verifyResetOtp(VerifyOtpRequest request) {
        User user = validateAndConsumeOtp(request.getPhoneNumber(), request.getOtp());

        // Invalidate previous active reset tokens for this user
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
        for (PasswordResetToken activeToken : activeTokens) {
            activeToken.setUsed(true);
            passwordResetTokenRepository.save(activeToken);
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = TokenHashUtil.sha256Hex(resetToken);

        PasswordResetToken resetTokenEntity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetTokenEntity);

        return ResetTokenResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    private User validateAndConsumeOtp(String phoneNumber, String otp) {
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

        boolean matches = (otpEntity.getOtpHash() != null && otpEntity.getOtpHash().equals(hashOtp(otp)))
                || (passwordEncoder != null && passwordEncoder.matches(otp, otpEntity.getOtpHash()))
                || otp.equals(otpEntity.getOtpHash());

        if (!matches) {
            if (otpEntity.getAttempts() >= otpProperties.getMaxAttempts()) {
                otpEntity.setUsed(true);
            }
            otpRepository.save(otpEntity);
            throw new OtpException("Invalid OTP");
        }

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new OtpException("No account found. Please register first."));

        if (!user.isEnabled() && !PENDING_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new OtpException("Account is not active");
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
                        otpProperties.getExpirationMinutes()
                );
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
