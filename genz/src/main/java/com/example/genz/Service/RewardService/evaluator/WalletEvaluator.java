package com.example.genz.service.RewardService.evaluator;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeRequirementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WalletEvaluator implements BadgeRequirementEvaluator {

    @Override
    public boolean supports(BadgeRequirementType requirementType) {
        return requirementType == BadgeRequirementType.FIRST_TRANSFER ||
               requirementType == BadgeRequirementType.FIRST_WITHDRAWAL ||
               requirementType == BadgeRequirementType.QR_PAYMENT ||
               requirementType == BadgeRequirementType.AUTO_SAVE_ACTIVATED ||
               requirementType == BadgeRequirementType.REWARD_REDEEMED ||
               requirementType == BadgeRequirementType.SMART_SPENDER;
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
                case FIRST_TRANSFER -> "transferCount";
                case FIRST_WITHDRAWAL -> "withdrawalCount";
                case QR_PAYMENT -> "qrPaymentCount";
                case AUTO_SAVE_ACTIVATED -> "autoSaveActive";
                case REWARD_REDEEMED -> "rewardRedeemedCount";
                case SMART_SPENDER -> "smartSpenderCount";
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
