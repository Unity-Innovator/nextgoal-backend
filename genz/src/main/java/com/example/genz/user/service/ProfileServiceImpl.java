package com.example.genz.user.service;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.level.service.LevelService;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.repository.UserBadgeRepository;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
}
