package com.example.genz.reward.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoalEvaluator implements BadgeRequirementEvaluator {

    private final GoalRepository goalRepository;

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.FIRST_GOAL_CREATED ||
               requirementType == BadgeRequirementType.GOALS_COMPLETED ||
               requirementType == BadgeRequirementType.MULTI_GOAL_MASTER;
    }

    @Override
    public boolean isRequirementMet(User user, Badge badge, Map<String, Object> context) {
        long currentProgress = calculateCurrentProgress(user, badge, context);
        int requiredValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return currentProgress >= requiredValue;
    }

    @Override
    public long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context) {
        if (badge.getRequirementType() == BadgeRequirementType.FIRST_GOAL_CREATED ||
            badge.getRequirementType() == BadgeRequirementType.MULTI_GOAL_MASTER) {
            long count = goalRepository.countByOwner(user);
            if (count == 0 && context != null && context.containsKey("createdGoalCount")) {
                Object cntObj = context.get("createdGoalCount");
                if (cntObj instanceof Number) {
                    count = ((Number) cntObj).longValue();
                }
            }
            return count;
        } else if (badge.getRequirementType() == BadgeRequirementType.GOALS_COMPLETED) {
            long completedCount = goalRepository.countByOwnerAndStatus(user, GoalStatus.COMPLETED);
            if (context != null && context.containsKey("completedGoalCount")) {
                Object cntObj = context.get("completedGoalCount");
                if (cntObj instanceof Number) {
                    completedCount = ((Number) cntObj).longValue();
                }
            }
            return completedCount;
        }
        return 0;
    }
}
