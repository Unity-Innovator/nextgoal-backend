package com.example.genz.auth.service;

import com.example.genz.auth.dto.SendOtpRequest;
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
    }
}
