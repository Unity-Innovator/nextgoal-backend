package com.example.genz.reward.dto;

import com.example.genz.reward.enums.RewardSize;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinRewardSizeResponse {
    private UUID id;
    private RewardSize size;
    private int stock;
}
