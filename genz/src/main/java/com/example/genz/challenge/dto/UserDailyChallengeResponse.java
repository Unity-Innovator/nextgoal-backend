package com.example.genz.challenge.dto;

import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyChallengeResponse {
    private UUID userDailyChallengeId;
    private UUID challengeId;
    private String title;
    private String description;
    private ChallengeType challengeType;
    private Integer xpReward;
    private ChallengeStatus status;
    private LocalDate challengeDate;
    private LocalDateTime completedAt;
}
