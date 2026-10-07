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

    public ProfileUserResponse toUserResponse(User user, int achievementUnlockedCount) {
        if (user == null) {
            return null;
        }
        String avatarUrl = user.getSettings() != null ? user.getSettings().getAvatarUrl() : null;
        return ProfileUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole() == null ? com.example.genz.auth.enums.UserRole.USER : user.getRole())
                .username(user.getProfileUsername())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(avatarUrl)
                .achievementUnlockedCount(achievementUnlockedCount)
                .build();
    }

    public ProfileUserResponse toUserResponse(User user) {
        return toUserResponse(user, 0);
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
            Integer coins,
            int achievementUnlockedCount
    ) {
        return ProfileResponse.builder()
                .user(toUserResponse(user, achievementUnlockedCount))
                .level(levelResponse)
                .summary(toSummaryResponse(totalSaved, totalGoals, coins))
                .build();
    }

    public ProfileResponse toProfileResponse(
            User user,
            ProfileLevelResponse levelResponse,
            BigDecimal totalSaved,
            Long totalGoals,
            Integer coins
    ) {
        return toProfileResponse(user, levelResponse, totalSaved, totalGoals, coins, 0);
    }
}
