package com.example.genz.dto.RewardDTO;

import com.example.genz.enums.ActivityType;
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
public class XpTransactionResponse {
    private UUID id;
    private ActivityType activityType;
    private UUID referenceId;
    private Integer points;
    private LocalDateTime createdAt;
}
