package com.example.genz.reward.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.enums.BadgeRequirementType;

import java.util.Map;

public interface BadgeRequirementEvaluator {
    boolean supports(BadgeRequirementType requirementType);
    boolean isRequirementMet(User user, Badge badge, Map<String, Object> context);
    long calculateCurrentProgress(User user, Badge badge, Map<String, Object> context);
}
