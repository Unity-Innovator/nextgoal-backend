package com.example.genz.wallet.dto;

import com.example.genz.goal.enums.GoalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepositResponse {
    private boolean success;
    private String message;
    private BigDecimal balance;
    private Integer coinsEarned;
    private BigDecimal currentAmount;
    private BigDecimal targetAmount;
    private Double progressPercentage;
    private GoalStatus goalStatus;
    private DepositData data;

    public DepositResponse(boolean success, String message, BigDecimal balance) {
        this.success = success;
        this.message = message;
        this.balance = balance;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepositData {
        private BigDecimal currentAmount;
        private BigDecimal targetAmount;
        private Double progressPercentage;
        private GoalStatus goalStatus;
        private Integer coinsEarned;
        private BigDecimal balance;
    }
}
