package com.example.genz.service.RewardService.evaluator;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeRequirementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EngagementEvaluator implements BadgeRequirementEvaluator {

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.PROFILE_COMPLETED ||
               requirementType == BadgeRequirementType.BANK_ACCOUNT_LINKED ||
               requirementType == BadgeRequirementType.DEBIT_CARD_ADDED ||
               requirementType == BadgeRequirementType.ACCOUNT_VERIFIED ||
               requirementType == BadgeRequirementType.EARLY_ACTIVITY ||
               requirementType == BadgeRequirementType.NIGHT_ACTIVITY ||
               requirementType == BadgeRequirementType.APP_EXPLORER ||
               requirementType == BadgeRequirementType.QUIZ_COMPLETED ||
               requirementType == BadgeRequirementType.FEEDBACK_GIVEN ||
               requirementType == BadgeRequirementType.ANNIVERSARY ||
               requirementType == BadgeRequirementType.LESSONS_COMPLETED;
    }

    @Override
    public boolean isRequirementMet(User user, Badge badge, Map<String, Object> context) {
        long currentProgress = calculateCurrentProgress(user, badge, context);
        int requiredValue = badge.getRequirementValue() != null ? badge.getRequirementValue() : 1;
        return currentProgress >= requiredValue;
    }

    @Override
    public long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context) {
        if (badge.getRequirementType() == BadgeRequirementType.PROFILE_COMPLETED) {
            boolean isComplete = user.getFullName() != null && user.getPhoneNumber() != null && user.getDateOfBirth() != null;
            return isComplete ? 1 : 0;
        }

        if (context != null) {
            String key = switch (badge.getRequirementType()) {
                case BANK_ACCOUNT_LINKED -> "bankAccountLinked";
                case DEBIT_CARD_ADDED -> "debitCardAdded";
                case ACCOUNT_VERIFIED -> "accountVerified";
                case EARLY_ACTIVITY -> "earlyBirdCount";
                case NIGHT_ACTIVITY -> "nightOwlCount";
                case APP_EXPLORER -> "appExplorerCount";
                case QUIZ_COMPLETED -> "quizCompletedCount";
                case FEEDBACK_GIVEN -> "feedbackGiven";
                case ANNIVERSARY -> "anniversaryMet";
                case LESSONS_COMPLETED -> "lessonsCompletedCount";
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
