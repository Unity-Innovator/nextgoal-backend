package com.example.genz.service.RewardService;

import com.example.genz.dto.RewardDTO.AchievementResponse;
import com.example.genz.dto.RewardDTO.UserRewardSummaryResponse;
import com.example.genz.dto.RewardDTO.XpTransactionResponse;
import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.UserBadge;
import com.example.genz.entity.Reward.UserReward;
import com.example.genz.enums.BadgeCategory;
import com.example.genz.enums.BadgeRequirementType;

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
