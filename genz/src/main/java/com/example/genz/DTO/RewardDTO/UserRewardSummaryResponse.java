package com.example.genz.DTO.RewardDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRewardSummaryResponse {
    private Integer totalXp;
    private Integer level;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long achievementCount;
    private Long totalAchievements;
    private Integer progressPercentage;
    private LocalDate lastActivityDate;
}
