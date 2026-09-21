package com.example.genz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileLevelResponse {
    private Integer level;
    private String name;
    private Integer totalXp;
    private Integer currentLevelXp;
    private Integer nextLevelXp;
    private Double progressPercentage;

    @Builder.Default
    private Integer completedAchievements = 0;

    @Builder.Default
    private Integer streak = 0;
}
