package com.example.genz.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementsResponse {
    private long total;
    private long completed;
    private List<AchievementResponse> achievements;
}
