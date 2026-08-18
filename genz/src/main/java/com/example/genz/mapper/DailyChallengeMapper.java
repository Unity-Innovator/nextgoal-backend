package com.example.genz.mapper;

import com.example.genz.dto.DailyChallengeDTO.UserDailyChallengeResponse;
import com.example.genz.entity.DailyChallenge.DailyChallenge;
import com.example.genz.entity.DailyChallenge.UserDailyChallenge;
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
