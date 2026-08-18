package com.example.genz.service.RewardService.evaluator;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeRequirementType;

import java.util.Map;

public interface BadgeRequirementEvaluator {
    boolean supports(BadgeRequirementType requirementType);
    boolean isRequirementMet(User user, Badge badge, Map<String, Object> context);
    long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context);
}
