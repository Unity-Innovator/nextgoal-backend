package com.example.genz.reward.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinRewardResponse {
    private UUID id;
    private String name;
    private String imageUrl;
    private String description;
    private int redeemCoins;
    private int stock;
    private boolean isActive;
    private List<CoinRewardSizeResponse> sizes;
}
