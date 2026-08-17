package com.example.genz.Service.RewardService.evaluator;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.Badge;
import com.example.genz.Entity.Reward.UserReward;
import com.example.genz.Enums.BadgeRequirementType;
import com.example.genz.Repository.RewardRepository.UserRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StreakEvaluator implements BadgeRequirementEvaluator {

    private final UserRewardRepository userRewardRepository;

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.STREAK_DAYS;
    }

    @Override
    public boolean isRequirementMet(User user, Badge badge, Map<String, Object> context) {
        long currentStreak = calculateCurrentProgress(user, badge, context);
        int requiredStreak = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return currentStreak >= requiredStreak;
    }

    @Override
    public long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context) {
        if (context != null && context.containsKey("streak")) {
            Object streakObj = context.get("streak");
            if (streakObj instanceof Number) {
                return ((Number) streakObj).longValue();
            }
        }
        return userRewardRepository.findByUser(user)
                .map(UserReward::getCurrentStreak)
                .orElse(0);
    }
}
