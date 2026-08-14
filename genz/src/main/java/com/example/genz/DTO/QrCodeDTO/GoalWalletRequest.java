package com.example.genz.dto.QrCodeDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoalWalletRequest {
    @NotBlank(message = "Goal ID is required")
    private String goalId;
}
