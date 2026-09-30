package com.example.genz.wallet.dto;

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
public class TopUpResponse {
    private UUID walletId;
    private String referenceId;
    private BigDecimal amount;
    private String currency;
    private String bank;
    private String paymentMethod;
    private BigDecimal balance;
    private BigDecimal newBalance;
    private LocalDateTime completedAt;
}
