package com.example.genz.challenge.mapper;

import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import org.springframework.stereotype.Component;

@Component
public class DailyChallengeMapper {

    public UserDailyChallengeResponse toUserDailyChallengeResponse(UserDailyChallenge udc) {
        if (udc == null) {
            return null;
        }
        DailyChallenge challenge = udc.getChallenge();
        return UserDailyChallengeResponse.builder()
                .userDailyChallengeId(udc.getId())
                .challengeId(challenge != null ? challenge.getId() : null)
                .title(challenge != null ? challenge.getTitle() : null)
                .description(challenge != null ? challenge.getDescription() : null)
                .challengeType(challenge != null ? challenge.getChallengeType() : null)
                .xpReward(challenge != null ? challenge.getXpReward() : null)
                .status(udc.getStatus())
                .challengeDate(udc.getChallengeDate())
                .completedAt(udc.getCompletedAt())
                .build();
    }
}
