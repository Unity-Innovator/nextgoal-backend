package com.example.genz.Service.RewardService;

import com.example.genz.DTO.RewardDTO.AchievementResponse;
import com.example.genz.DTO.RewardDTO.UserRewardSummaryResponse;
import com.example.genz.DTO.RewardDTO.XpTransactionResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.Badge;
import com.example.genz.Entity.Reward.UserBadge;
import com.example.genz.Entity.Reward.UserReward;
import com.example.genz.Entity.Reward.XpTransaction;
import com.example.genz.Enums.ActivityType;
import com.example.genz.Enums.BadgeCategory;
import com.example.genz.Enums.BadgeRequirementType;
import com.example.genz.Enums.NotificationType;
import com.example.genz.mapper.AchievementMapper;
import com.example.genz.Repository.RewardRepository.BadgeRepository;
import com.example.genz.Repository.RewardRepository.UserBadgeRepository;
import com.example.genz.Repository.RewardRepository.UserRewardRepository;
import com.example.genz.Repository.RewardRepository.XpTransactionRepository;
import com.example.genz.Service.NotificationService.NotificationService;
import com.example.genz.Service.RewardService.evaluator.BadgeEvaluatorRegistry;
import com.example.genz.Service.RewardService.evaluator.BadgeRequirementEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final BadgeEvaluatorRegistry evaluatorRegistry;
    private final NotificationService notificationService;
    private final AchievementMapper achievementMapper;

    @Override
    @Transactional
    public List<UserBadge> evaluateAndUnlockAchievements(User user, BadgeRequirementType requirementType, Map<String, Object> context) {
        if (user == null || requirementType == null) {
            return Collections.emptyList();
        }

        List<Badge> candidateBadges = badgeRepository.findByRequirementTypeAndActiveTrue(requirementType);
        if (candidateBadges.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserBadge> newlyUnlocked = new ArrayList<>();
        UserReward userReward = getOrCreateUserReward(user);

        if (requirementType == BadgeRequirementType.STREAK_DAYS && context != null && context.containsKey("streak")) {
            Object streakObj = context.get("streak");
            if (streakObj instanceof Number) {
                int streakVal = ((Number) streakObj).intValue();
                userReward.setCurrentStreak(streakVal);
                if (userReward.getLongestStreak() == null || streakVal > userReward.getLongestStreak()) {
                    userReward.setLongestStreak(streakVal);
                }
                userRewardRepository.save(userReward);
            }
        }

        for (Badge badge : candidateBadges) {
            // Check if user already unlocked this badge
            if (userBadgeRepository.existsByUserAndBadge(user, badge)) {
                continue;
            }

            Optional<BadgeRequirementEvaluator> evaluatorOpt = evaluatorRegistry.getEvaluator(badge.getRequirementType());
            if (evaluatorOpt.isPresent() && evaluatorOpt.get().isRequirementMet(user, badge, context)) {
                // 1. Create and Save UserBadge
                UserBadge userBadge = UserBadge.builder()
                        .user(user)
                        .badge(badge)
                        .build();
                userBadge = userBadgeRepository.save(userBadge);

                // 2. Award XP & update UserReward
                int points = badge.getXpReward() != null ? badge.getXpReward() : 0;
                userReward.addXp(points);
                userRewardRepository.save(userReward);

                // 3. Create XP Transaction (if not already exists for this badge)
                if (!xpTransactionRepository.existsByUserAndActivityTypeAndReferenceId(user, ActivityType.ACHIEVEMENT_REWARD, badge.getId())) {
                    XpTransaction xpTransaction = XpTransaction.builder()
                            .user(user)
                            .activityType(ActivityType.ACHIEVEMENT_REWARD)
                            .referenceId(badge.getId())
                            .points(points)
                            .build();
                    xpTransactionRepository.save(xpTransaction);
                }

                // 4. Create Notification
                try {
                    notificationService.createNotification(
                            user,
                            "🏆 New Achievement!",
                            badge.getName() + " (+" + points + " XP)",
                            NotificationType.ACHIEVEMENT_UNLOCKED,
                            badge.getId()
                    );
                } catch (Exception ignored) {
                    // Fail-safe logging / notification creation handle
                }

                newlyUnlocked.add(userBadge);
            }
        }

        return newlyUnlocked;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievementsForUser(User user) {
        List<Badge> allBadges = badgeRepository.findByActiveTrue();
        return buildAchievementResponses(user, allBadges);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getUnlockedAchievementsForUser(User user) {
        List<UserBadge> userBadges = userBadgeRepository.findByUserOrderByEarnedAtDesc(user);
        return userBadges.stream().map(ub -> {
            Badge b = ub.getBadge();
            int reqVal = b.getRequirementValue() != null ? b.getRequirementValue() : 1;
            return achievementMapper.toAchievementResponse(b, true, (long) reqVal, 100, ub.getEarnedAt());
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievementsByCategoryForUser(User user, BadgeCategory category) {
        List<Badge> categoryBadges = badgeRepository.findByCategoryAndActiveTrue(category);
        return buildAchievementResponses(user, categoryBadges);
    }

    @Override
    @Transactional
    public UserRewardSummaryResponse getUserRewardSummary(User user) {
        UserReward reward = getOrCreateUserReward(user);
        long unlockedCount = userBadgeRepository.countByUser(user);
        long totalBadges = badgeRepository.count();

        int progressPercentage = totalBadges > 0 ? (int) Math.min(100, (unlockedCount * 100) / totalBadges) : 0;

        return achievementMapper.toUserRewardSummaryResponse(reward, unlockedCount, totalBadges, progressPercentage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<XpTransactionResponse> getXpHistory(User user) {
        List<XpTransaction> transactions = xpTransactionRepository.findByUserOrderByCreatedAtDesc(user);
        return achievementMapper.toXpTransactionResponseList(transactions);
    }

    @Override
    @Transactional
    public UserReward getOrCreateUserReward(User user) {
        return userRewardRepository.findByUser(user)
                .orElseGet(() -> userRewardRepository.save(UserReward.builder()
                        .user(user)
                        .totalXp(0)
                        .level(1)
                        .currentStreak(0)
                        .longestStreak(0)
                        .build()));
    }

    private List<AchievementResponse> buildAchievementResponses(User user, List<Badge> badges) {
        List<UserBadge> unlockedUserBadges = userBadgeRepository.findByUserOrderByEarnedAtDesc(user);
        Map<UUID, UserBadge> unlockedMap = unlockedUserBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub, (e1, e2) -> e1));

        return badges.stream().map(badge -> {
            boolean isUnlocked = unlockedMap.containsKey(badge.getId());
            UserBadge ub = unlockedMap.get(badge.getId());

            int reqValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
            long currentProgress;
            int percentage;

            if (isUnlocked) {
                currentProgress = reqValue;
                percentage = 100;
            } else {
                Optional<BadgeRequirementEvaluator> evaluator = evaluatorRegistry.getEvaluator(badge.getRequirementType());
                currentProgress = evaluator.map(e -> e.calculateCurrentProgress(user, badge, null)).orElse(0L);
                percentage = reqValue > 0 ? (int) Math.min(100, (currentProgress * 100) / reqValue) : 0;
            }

            return achievementMapper.toAchievementResponse(
                    badge,
                    isUnlocked,
                    currentProgress,
                    percentage,
                    ub != null ? ub.getEarnedAt() : null
            );
        }).collect(Collectors.toList());
    }
}
