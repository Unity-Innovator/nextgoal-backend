package com.example.genz.reward.mapper;

import com.example.genz.reward.dto.*;
import com.example.genz.reward.entity.CoinReward;
import com.example.genz.reward.entity.CoinRewardSize;
import com.example.genz.reward.entity.RewardRedemption;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoinRewardMapper {

    public CoinRewardSizeResponse toCoinRewardSizeResponse(CoinRewardSize size) {
        if (size == null) {
            return null;
        }
        return CoinRewardSizeResponse.builder()
                .id(size.getId())
                .size(size.getSize())
                .stock(size.getStock())
                .build();
    }

    public List<CoinRewardSizeResponse> toCoinRewardSizeResponseList(List<CoinRewardSize> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return Collections.emptyList();
        }
        return sizes.stream()
                .map(this::toCoinRewardSizeResponse)
                .collect(Collectors.toList());
    }

    public CoinRewardResponse toCoinRewardResponse(CoinReward reward) {
        if (reward == null) {
            return null;
        }
        return CoinRewardResponse.builder()
                .id(reward.getId())
                .name(reward.getName())
                .imageUrl(reward.getImageUrl())
                .description(reward.getDescription())
                .redeemCoins(reward.getRedeemCoins())
                .stock(reward.getStock())
                .isActive(reward.isActive())
                .sizes(toCoinRewardSizeResponseList(reward.getCoinRewardSizes()))
                .build();
    }

    public List<CoinRewardResponse> toCoinRewardResponseList(List<CoinReward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return Collections.emptyList();
        }
        return rewards.stream()
                .map(this::toCoinRewardResponse)
                .collect(Collectors.toList());
    }

    public RedemptionResponse toRedemptionResponse(RewardRedemption redemption, int remainingCoins) {
        if (redemption == null) {
            return null;
        }
        return RedemptionResponse.builder()
                .referenceNumber(redemption.getReferenceNumber())
                .item(redemption.getItemName())
                .size(redemption.getSelectedSize())
                .coinsUsed(redemption.getCoinsUsed())
                .remainingCoins(remainingCoins)
                .status(redemption.getStatus())
                .estimatedDeliveryFrom(redemption.getEstimatedDeliveryFrom())
                .estimatedDeliveryTo(redemption.getEstimatedDeliveryTo())
                .build();
    }

    public RedemptionHistoryResponse toRedemptionHistoryResponse(RewardRedemption redemption) {
        if (redemption == null) {
            return null;
        }
        return RedemptionHistoryResponse.builder()
                .id(redemption.getId())
                .referenceNumber(redemption.getReferenceNumber())
                .item(redemption.getItemName())
                .size(redemption.getSelectedSize())
                .coinsUsed(redemption.getCoinsUsed())
                .status(redemption.getStatus())
                .estimatedDeliveryFrom(redemption.getEstimatedDeliveryFrom())
                .estimatedDeliveryTo(redemption.getEstimatedDeliveryTo())
                .createdAt(redemption.getCreatedAt())
                .build();
    }

    public List<RedemptionHistoryResponse> toRedemptionHistoryResponseList(List<RewardRedemption> redemptions) {
        if (redemptions == null || redemptions.isEmpty()) {
            return Collections.emptyList();
        }
        return redemptions.stream()
                .map(this::toRedemptionHistoryResponse)
                .collect(Collectors.toList());
    }
}
