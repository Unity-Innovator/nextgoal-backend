package com.example.genz.mapper;

import com.example.genz.dto.RewardDTO.AchievementResponse;
import com.example.genz.dto.RewardDTO.UserRewardSummaryResponse;
import com.example.genz.dto.RewardDTO.XpTransactionResponse;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.entity.Reward.UserReward;
import com.example.genz.entity.Reward.XpTransaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AchievementMapper {

    public AchievementResponse toAchievementResponse(
            Badge badge,
            boolean isUnlocked,
            long currentProgress,
            int progressPercentage,
            LocalDateTime earnedAt
    ) {
        if (badge == null) {
            return null;
        }
        int reqValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return AchievementResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconUrl(badge.getIconUrl())
                .category(badge.getCategory())
                .rarity(badge.getRarity())
                .requirementType(badge.getRequirementType())
                .xpReward(badge.getXpReward())
                .requirementValue(reqValue)
                .currentProgress(currentProgress)
                .progressPercentage(progressPercentage)
                .unlocked(isUnlocked)
                .earnedAt(earnedAt)
                .build();
    }

    public UserRewardSummaryResponse toUserRewardSummaryResponse(
            UserReward reward,
            long unlockedCount,
            long totalBadges,
            int progressPercentage
    ) {
        if (reward == null) {
            return null;
        }
        return UserRewardSummaryResponse.builder()
                .totalXp(reward.getTotalXp())
                .level(reward.getLevel())
                .currentStreak(reward.getCurrentStreak())
                .longestStreak(reward.getLongestStreak())
                .achievementCount(unlockedCount)
                .totalAchievements(totalBadges)
                .progressPercentage(progressPercentage)
                .lastActivityDate(reward.getLastActivityDate())
                .build();
    }

    public XpTransactionResponse toXpTransactionResponse(XpTransaction tx) {
        if (tx == null) {
            return null;
        }
        return XpTransactionResponse.builder()
                .id(tx.getId())
                .activityType(tx.getActivityType())
                .referenceId(tx.getReferenceId())
                .points(tx.getPoints())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    public List<XpTransactionResponse> toXpTransactionResponseList(List<XpTransaction> transactions) {
        if (transactions == null) {
            return Collections.emptyList();
        }
        return transactions.stream()
                .map(this::toXpTransactionResponse)
                .collect(Collectors.toList());
    }
}
