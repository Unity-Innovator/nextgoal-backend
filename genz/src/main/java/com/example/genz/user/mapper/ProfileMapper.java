package com.example.genz.user.mapper;

import com.example.genz.auth.entity.User;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.dto.ProfileSummaryResponse;
import com.example.genz.user.dto.ProfileUserResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProfileMapper {

    public ProfileUserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return ProfileUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .build();
    }

    public ProfileSummaryResponse toSummaryResponse(BigDecimal totalSaved, Long totalGoals, Integer coins) {
        return ProfileSummaryResponse.builder()
                .totalSaved(totalSaved)
                .totalGoals(totalGoals)
                .coins(coins)
                .build();
    }

    public ProfileResponse toProfileResponse(
            User user,
            ProfileLevelResponse levelResponse,
            BigDecimal totalSaved,
            Long totalGoals,
            Integer coins
    ) {
        return ProfileResponse.builder()
                .user(toUserResponse(user))
                .level(levelResponse)
                .summary(toSummaryResponse(totalSaved, totalGoals, coins))
                .build();
    }
}
