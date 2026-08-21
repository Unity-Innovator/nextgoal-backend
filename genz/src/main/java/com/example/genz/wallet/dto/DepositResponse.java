package com.example.genz.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DepositResponse {
    private boolean success;
    private String message;
    private BigDecimal balance;
}
