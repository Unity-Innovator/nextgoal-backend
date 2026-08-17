package com.example.genz.DTO.RewardDTO;

import com.example.genz.Enums.BadgeCategory;
import com.example.genz.Enums.BadgeRarity;
import com.example.genz.Enums.BadgeRequirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private BadgeCategory category;
    private BadgeRarity rarity;
    private BadgeRequirementType requirementType;
    private Integer xpReward;
    private Integer requirementValue;
    private Long currentProgress;
    private Integer progressPercentage;
    private Boolean unlocked;
    private LocalDateTime earnedAt;
}
