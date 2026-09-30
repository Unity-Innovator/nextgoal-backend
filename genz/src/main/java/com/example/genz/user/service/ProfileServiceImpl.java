package com.example.genz.user.service;

import com.example.genz.auth.entity.RefreshToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.RefreshTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.level.service.LevelService;
import com.example.genz.reward.repository.UserBadgeRepository;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.repository.UserSettingsRepository;
import com.example.genz.settings.service.UserSettingsService;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.dto.UpdateProfileRequest;
import com.example.genz.user.dto.UpdateProfileResponse;
import com.example.genz.user.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final LevelService levelService;
    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ChallengeCompletionService challengeCompletionService;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final UserSettingsService userSettingsService;
    private final UserSettingsRepository userSettingsRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public ProfileResponse getUserProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        int storedXp = user.getTotalXp() != null ? user.getTotalXp() : 0;
        int txXp = (xpTransactionRepository != null && user.getId() != null)
                ? xpTransactionRepository.sumPointsByUser(user)
                : 0;
        int totalXp = Math.max(storedXp, txXp);
        if (storedXp < totalXp) {
            user.setTotalXp(totalXp);
            if (userRepository != null) {
                userRepository.save(user);
            }
        }

        ProfileLevelResponse levelResponse = levelService.calculateLevelProgress(totalXp);

        BigDecimal rawTotalSaved = goalContributionRepository.sumAmountByUserId(user.getId());
        BigDecimal totalSaved = (rawTotalSaved != null ? rawTotalSaved : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        long totalGoals = goalRepository.countByOwner(user);
        int coins = user.getCoinBalance() != null ? user.getCoinBalance() : 0;
        int achievementUnlockedCount = (int) userBadgeRepository.countByUser(user);

        int streak = challengeCompletionService.calculateStreak(user);
        if (userRewardRepository != null) {
            userRewardRepository.findByUser(user).ifPresent(ur -> {
                if (ur.getCurrentStreak() != null && ur.getCurrentStreak() != streak) {
                    ur.setCurrentStreak(streak);
                    userRewardRepository.save(ur);
                }
            });
        }

        if (levelResponse != null) {
            levelResponse.setCompletedAchievements(achievementUnlockedCount);
            levelResponse.setStreak(streak);
        }

        return profileMapper.toProfileResponse(
                user,
                levelResponse,
                totalSaved,
                totalGoals,
                coins,
                achievementUnlockedCount
        );
    }

    @Override
    @Transactional
    public UpdateProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (request == null) {
            throw new BadRequestException("Update request cannot be null");
        }

        boolean phoneChanged = false;

        if (request.getFullName() != null) {
            String trimmedName = request.getFullName().trim();
            if (trimmedName.isEmpty()) {
                throw new BadRequestException("Full name cannot be blank");
            }
            user.setFullName(trimmedName);
        }

        if (request.getUsername() != null) {
            String normalizedUsername = normalizeUsername(request.getUsername());
            if (normalizedUsername != null && !normalizedUsername.isEmpty()) {
                boolean conflict = (user.getId() != null)
                        ? userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, user.getId())
                        : userRepository.existsByUsernameIgnoreCase(normalizedUsername);
                if (conflict) {
                    throw new BadRequestException("Username already exists");
                }
                user.setUsername(normalizedUsername);
            } else {
                user.setUsername(null);
            }
        }

        if (request.getPhoneNumber() != null) {
            String trimmedPhone = request.getPhoneNumber().trim();
            if (trimmedPhone.isEmpty()) {
                throw new BadRequestException("Phone number cannot be blank");
            }
            if (!trimmedPhone.equals(user.getPhoneNumber())) {
                boolean conflict = (user.getId() != null)
                        ? userRepository.existsByPhoneNumberAndIdNot(trimmedPhone, user.getId())
                        : userRepository.existsByPhoneNumber(trimmedPhone);
                if (conflict) {
                    throw new BadRequestException("Phone number already exists");
                }
                user.setPhoneNumber(trimmedPhone);
                phoneChanged = true;
            }
        }

        if (userRepository != null) {
            user = userRepository.save(user);
        }

        String currentAvatarUrl = user.getSettings() != null ? user.getSettings().getAvatarUrl() : null;
        if (request.getAvatarUrl() != null && userSettingsService != null) {
            UserSettings settings = userSettingsService.getOrCreateUserSettings(user);
            String trimmedAvatar = request.getAvatarUrl().trim();
            settings.setAvatarUrl(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
            if (userSettingsRepository != null) {
                settings = userSettingsRepository.save(settings);
            }
            user.setSettings(settings);
            currentAvatarUrl = settings.getAvatarUrl();
        } else if (currentAvatarUrl == null && userSettingsRepository != null && user.getId() != null) {
            currentAvatarUrl = userSettingsRepository.findByUser(user)
                    .map(UserSettings::getAvatarUrl)
                    .orElse(null);
        }

        String newAccessToken = null;
        String newRefreshToken = null;
        String tokenType = null;
        Long expiresIn = null;

        if (phoneChanged && jwtUtil != null) {
            if (refreshTokenRepository != null) {
                refreshTokenRepository.revokeAllForUser(user);
            }
            newAccessToken = jwtUtil.generateAccessToken(user.getPhoneNumber());
            newRefreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());
            tokenType = "Bearer";
            expiresIn = jwtUtil.getAccessExpirationSeconds();

            if (refreshTokenRepository != null) {
                String tokenHash = TokenHashUtil.sha256Hex(newRefreshToken);
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
            }
        }

        return UpdateProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getProfileUsername())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(currentAvatarUrl)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .build();
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            return null;
        }
        String trimmed = rawUsername.trim();
        while (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }
}
