package com.example.genz.transaction.dto;

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
public class TransactionResponse {
    private UUID id;
    private TransactionUserResponse user;
    private String type;
    private BigDecimal amount;
    private String direction;
    private UUID goalId;
    private String goalName;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String currency;
    private String status;
    private String description;
    private LocalDateTime createdAt;
}
