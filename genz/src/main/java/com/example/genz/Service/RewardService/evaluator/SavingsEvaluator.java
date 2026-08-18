package com.example.genz.service.RewardService.evaluator;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeRequirementType;
import com.example.genz.repository.GoalRepository.GoalContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SavingsEvaluator implements BadgeRequirementEvaluator {

    private final GoalContributionRepository goalContributionRepository;

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.TOTAL_SAVED ||
               requirementType == BadgeRequirementType.FIRST_DEPOSIT ||
               requirementType == BadgeRequirementType.FIRST_CONTRIBUTION;
    }

    @Override
    public boolean isRequirementMet(User user, Badge badge, Map<String, Object> context) {
        long currentProgress = calculateCurrentProgress(user, badge, context);
        int requiredValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return currentProgress >= requiredValue;
    }

    @Override
    public long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context) {
        if (badge.getRequirementType() == BadgeRequirementType.FIRST_DEPOSIT ||
            badge.getRequirementType() == BadgeRequirementType.FIRST_CONTRIBUTION) {
            long count = goalContributionRepository.countByUserId(user.getId());
            if (count == 0 && context != null && context.containsKey("depositCount")) {
                Object depObj = context.get("depositCount");
                if (depObj instanceof Number) {
                    count = ((Number) depObj).longValue();
                }
            }
            return count;
        }

        BigDecimal totalSaved = goalContributionRepository.sumAmountByUserId(user.getId());
        if (context != null && context.containsKey("totalSaved")) {
            Object savedObj = context.get("totalSaved");
            if (savedObj instanceof Number) {
                totalSaved = BigDecimal.valueOf(((Number) savedObj).doubleValue());
            }
        }
        return totalSaved.longValue();
    }
}
