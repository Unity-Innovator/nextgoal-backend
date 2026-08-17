package com.example.genz.Service.RewardService.evaluator;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.Badge;
import com.example.genz.Enums.BadgeRequirementType;

import java.util.Map;

public interface BadgeRequirementEvaluator {
    boolean supports(BadgeRequirementType requirementType);
    boolean isRequirementMet(User user, Badge badge, Map<String, Object> context);
    long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context);
}
