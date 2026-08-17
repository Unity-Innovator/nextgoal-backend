package com.example.genz.Service.RewardService;

import com.example.genz.DTO.RewardDTO.AchievementResponse;
import com.example.genz.DTO.RewardDTO.UserRewardSummaryResponse;
import com.example.genz.DTO.RewardDTO.XpTransactionResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.UserBadge;
import com.example.genz.Entity.Reward.UserReward;
import com.example.genz.Enums.BadgeCategory;
import com.example.genz.Enums.BadgeRequirementType;

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
