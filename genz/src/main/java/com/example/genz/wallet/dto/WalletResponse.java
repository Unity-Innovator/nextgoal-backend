package com.example.genz.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private UUID walletId;
    private BigDecimal balance;
    private String currency;
    private Double monthChangePercent;
    private BigDecimal monthChangeAmount;
}
