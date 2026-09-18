package com.example.genz.reward.dto;

import com.example.genz.reward.enums.RewardRedemptionStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionHistoryResponse {
    private UUID id;
    private String referenceNumber;
    private String item;
    private String size;
    private int coinsUsed;
    private RewardRedemptionStatus status;
    private LocalDate estimatedDeliveryFrom;
    private LocalDate estimatedDeliveryTo;
    private LocalDateTime createdAt;
}
