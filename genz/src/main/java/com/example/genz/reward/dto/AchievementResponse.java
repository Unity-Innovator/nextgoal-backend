package com.example.genz.reward.dto;

import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.reward.enums.BadgeRarity;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("rewardXp")
    public Integer getRewardXp() {
        return xpReward;
    }

    @JsonProperty("target")
    public Integer getTarget() {
        return requirementValue;
    }

    @JsonProperty("progress")
    public Long getProgress() {
        return currentProgress;
    }

    @JsonProperty("completed")
    public Boolean getCompleted() {
        return unlocked;
    }

    @JsonProperty("completedAt")
    public LocalDateTime getCompletedAt() {
        return earnedAt;
    }

    @JsonProperty("icon")
    public String getIcon() {
        return iconUrl;
    }
}
