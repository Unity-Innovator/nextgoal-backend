package com.example.genz.DTO.DailyChallengeDTO;

import com.example.genz.Enums.ChallengeStatus;
import com.example.genz.Enums.ChallengeType;
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
