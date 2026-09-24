package com.example.genz.challenge.dto;

import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyChallengeResponse {
    private UUID id;
    private UUID userDailyChallengeId;
    private UUID challengeId;
    private String title;
    private String description;
    private ChallengeType type;
    private ChallengeType challengeType;
    private BigDecimal targetValue;
    private BigDecimal progress;
    private Integer rewardCoins;
    private ChallengeRuleType ruleType;
    private BigDecimal minimumAmount;
    private Integer targetCount;
    private Integer xpReward;
    private ChallengeStatus status;
    private Boolean completed;
    private Integer currentStreak;
    private LocalDate challengeDate;
    private LocalDateTime completedAt;
}
