package com.example.genz.reward.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.reward.dto.CoinRewardResponse;
import com.example.genz.reward.dto.RedeemRewardRequest;
import com.example.genz.reward.dto.RedemptionHistoryResponse;
import com.example.genz.reward.dto.RedemptionResponse;
import com.example.genz.reward.service.CoinRewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class CoinRewardController {

    private final CoinRewardService coinRewardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoinRewardResponse>>> getAllActiveRewards() {
        List<CoinRewardResponse> response = coinRewardService.getAllActiveRewards();
        return ResponseEntity.ok(ApiResponse.success("Rewards retrieved successfully", response));
    }

    @GetMapping("/{rewardId:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<ApiResponse<CoinRewardResponse>> getRewardById(@PathVariable UUID rewardId) {
        CoinRewardResponse response = coinRewardService.getRewardById(rewardId);
        return ResponseEntity.ok(ApiResponse.success("Reward retrieved successfully", response));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<RedemptionResponse>> redeemReward(
            @Valid @RequestBody RedeemRewardRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        RedemptionResponse response = coinRewardService.redeemReward(request, phoneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reward redeemed successfully", response));
    }

    @GetMapping("/redemptions")
    public ResponseEntity<ApiResponse<List<RedemptionHistoryResponse>>> getRedemptionHistory(
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        List<RedemptionHistoryResponse> response = coinRewardService.getRedemptionHistory(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Redemption history retrieved successfully", response));
    }
}
