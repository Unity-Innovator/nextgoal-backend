package com.example.genz.service.RewardService.evaluator;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeRequirementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpecialSeasonalEvaluator implements BadgeRequirementEvaluator {

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.SPECIAL_NEW_YEAR ||
               requirementType == BadgeRequirementType.SPECIAL_HOLIDAY ||
               requirementType == BadgeRequirementType.CHALLENGE_COMPLETED ||
               requirementType == BadgeRequirementType.PERFECT_MONTH ||
               requirementType == BadgeRequirementType.REFERRAL_ROYALTY ||
               requirementType == BadgeRequirementType.LEGEND_STATUS;
    }

    @Override
    public boolean isRequirementMet(User user, Badge badge, Map<String, Object> context) {
        long currentProgress = calculateCurrentProgress(user, badge, context);
        int requiredValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return currentProgress >= requiredValue;
    }

    @Override
    public long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context) {
        if (context != null) {
            String key = switch (badge.getRequirementType()) {
                case SPECIAL_NEW_YEAR -> "newYearGoalCreated";
                case SPECIAL_HOLIDAY -> "holidayHeroCompleted";
                case CHALLENGE_COMPLETED -> "challengeCompletedCount";
                case PERFECT_MONTH -> "perfectMonthAchieved";
                case REFERRAL_ROYALTY -> "referralRoyaltyCount";
                case LEGEND_STATUS -> "legendStatusMet";
                default -> "count";
            };
            if (context.containsKey(key)) {
                Object val = context.get(key);
                if (val instanceof Boolean) {
                    return ((Boolean) val) ? 1 : 0;
                }
                if (val instanceof Number) {
                    return ((Number) val).longValue();
                }
            }
        }
        return 0;
    }
}
