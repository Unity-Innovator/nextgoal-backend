package com.example.genz.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WithdrawResponse {
    private boolean success;
    private String message;
    private UUID walletId;
    private String referenceId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String bank;
    private BigDecimal balance;
    private BigDecimal newBalance;
    private BigDecimal remainingBalance;
    private LocalDateTime completedAt;
}
