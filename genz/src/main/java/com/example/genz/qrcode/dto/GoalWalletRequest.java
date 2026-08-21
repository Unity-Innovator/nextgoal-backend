package com.example.genz.qrcode.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoalWalletRequest {
    @NotBlank(message = "Goal ID is required")
    private String goalId;
}
