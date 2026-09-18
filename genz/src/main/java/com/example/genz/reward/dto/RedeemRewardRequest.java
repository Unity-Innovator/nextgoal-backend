package com.example.genz.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemRewardRequest {

    @NotNull(message = "Reward ID is required")
    private UUID rewardId;

    private UUID sizeId;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Delivery address is required")
    private String address;
}
