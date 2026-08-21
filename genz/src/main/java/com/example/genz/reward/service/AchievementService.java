package com.example.genz.reward.service;

import com.example.genz.reward.dto.AchievementResponse;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.dto.XpTransactionResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.UserBadge;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.reward.enums.BadgeRequirementType;

import java.util.List;
import java.util.Map;

public interface AchievementService {
    List<UserBadge> evaluateAndUnlockAchievements(User user, BadgeRequirementType requirementType, Map<String, Object> context);
    List<AchievementResponse> getAllAchievementsForUser(User user);
    List<AchievementResponse> getUnlockedAchievementsForUser(User user);
    List<AchievementResponse> getAchievementsByCategoryForUser(User user, BadgeCategory category);
    UserRewardSummaryResponse getUserRewardSummary(User user);
    List<XpTransactionResponse> getXpHistory(User user);
    UserReward getOrCreateUserReward(User user);
}
