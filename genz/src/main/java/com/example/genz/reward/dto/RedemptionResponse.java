package com.example.genz.reward.dto;

import com.example.genz.reward.enums.RewardRedemptionStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionResponse {
    private String referenceNumber;
    private String item;
    private String size;
    private int coinsUsed;
    private int remainingCoins;
    private RewardRedemptionStatus status;
    private LocalDate estimatedDeliveryFrom;
    private LocalDate estimatedDeliveryTo;
}
