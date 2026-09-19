package com.example.genz.challenge.mapper;

import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import org.springframework.stereotype.Component;

@Component
public class DailyChallengeMapper {

    public UserDailyChallengeResponse toUserDailyChallengeResponse(UserDailyChallenge udc) {
        return toUserDailyChallengeResponse(udc, null);
    }

    public UserDailyChallengeResponse toUserDailyChallengeResponse(UserDailyChallenge udc, Integer currentStreak) {
        if (udc == null) {
            return null;
        }
        DailyChallenge challenge = udc.getChallenge();
        ChallengeRule rule = challenge != null ? challenge.getRule() : null;
        boolean isCompleted = udc.getStatus() == ChallengeStatus.COMPLETED;

        return UserDailyChallengeResponse.builder()
                .userDailyChallengeId(udc.getId())
                .challengeId(challenge != null ? challenge.getId() : null)
                .title(challenge != null ? challenge.getTitle() : null)
                .description(challenge != null ? challenge.getDescription() : null)
                .challengeType(challenge != null ? challenge.getChallengeType() : null)
                .ruleType(rule != null ? rule.getRuleType() : null)
                .minimumAmount(rule != null ? rule.getMinimumAmount() : null)
                .targetCount(rule != null ? rule.getTargetCount() : null)
                .xpReward(challenge != null ? challenge.getXpReward() : null)
                .status(udc.getStatus())
                .completed(isCompleted)
                .currentStreak(currentStreak)
                .challengeDate(udc.getChallengeDate())
                .completedAt(udc.getCompletedAt())
                .build();
    }
}
