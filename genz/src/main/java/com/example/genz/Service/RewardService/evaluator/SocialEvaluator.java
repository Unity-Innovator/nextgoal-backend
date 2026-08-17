package com.example.genz.Service.RewardService.evaluator;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.Badge;
import com.example.genz.Enums.BadgeRequirementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SocialEvaluator implements BadgeRequirementEvaluator {

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.FRIEND_INVITED ||
               requirementType == BadgeRequirementType.FIRST_POST ||
               requirementType == BadgeRequirementType.HELPFUL_ACTIONS ||
               requirementType == BadgeRequirementType.COMMUNITY_CONTRIBUTIONS;
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
                case FRIEND_INVITED -> "friendInvitedCount";
                case FIRST_POST -> "postCount";
                case HELPFUL_ACTIONS -> "helpfulActionCount";
                case COMMUNITY_CONTRIBUTIONS -> "communityContributionCount";
                default -> "count";
            };
            if (context.containsKey(key)) {
                Object val = context.get(key);
                if (val instanceof Number) {
                    return ((Number) val).longValue();
                }
            }
        }
        return 0;
    }
}
