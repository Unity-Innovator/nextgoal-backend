package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.SendOtpRequest;
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
    private final VerificationTokenRepository verificationTokenRepository;
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

        // Deliver to console (your flow)
        deliverOtp(phoneNumber, otp, purpose);

        // Deliver to email
        String recipientEmail = request.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = userRepository.findByPhoneNumber(phoneNumber)
                    .map(User::getEmail)
                    .orElse(null);
        }

        if (recipientEmail != null && !recipientEmail.isBlank() && emailOtpSender != null) {
            try {
                emailOtpSender.sendOtp(
                        recipientEmail,
                        otp,
                        purpose,
                        otpProperties.getExpirationMinutes());
            } catch (Exception e) {
                log.warn("Failed to send email OTP to {}: {}", recipientEmail, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        if (request.getPurpose() == null) {
            throw new OtpException("Purpose is required");
        }

        String phoneNumber = request.getPhoneNumber();
        User user = validateAndConsumeOtp(phoneNumber, request.getOtp(), request.getPurpose());

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

    private User validateAndConsumeOtp(String phoneNumber, String otp, OtpPurpose purpose) {
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
                .orElse(null);

        if (user != null && !user.isEnabled() && !PENDING_STATUS.equalsIgnoreCase(user.getStatus())) {
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
