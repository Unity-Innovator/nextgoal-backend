package com.example.genz.reward.mapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.genz.reward.dto.AchievementResponse;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.dto.XpTransactionResponse;
import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.entity.XpTransaction;

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
