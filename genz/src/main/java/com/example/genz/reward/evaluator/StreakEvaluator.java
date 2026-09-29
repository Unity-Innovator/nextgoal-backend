package com.example.genz.reward.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.example.genz.reward.repository.UserRewardRepository;
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
                .map(reward -> {
                    if (reward.getLastActivityDate() != null && reward.getLastActivityDate().isBefore(java.time.LocalDate.now().minusDays(1))) {
                        return 0L;
                    }
                    return reward.getCurrentStreak() != null ? reward.getCurrentStreak().longValue() : 0L;
                })
                .orElse(0L);
    }
}
