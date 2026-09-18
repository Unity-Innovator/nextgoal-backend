package com.example.genz.reward.service;

import com.example.genz.reward.dto.CoinRewardResponse;
import com.example.genz.reward.dto.RedeemRewardRequest;
import com.example.genz.reward.dto.RedemptionHistoryResponse;
import com.example.genz.reward.dto.RedemptionResponse;

import java.util.List;
import java.util.UUID;

public interface CoinRewardService {

    List<CoinRewardResponse> getAllActiveRewards();

    CoinRewardResponse getRewardById(UUID rewardId);

    RedemptionResponse redeemReward(RedeemRewardRequest request, String phoneNumber);

    List<RedemptionHistoryResponse> getRedemptionHistory(String phoneNumber);
}
