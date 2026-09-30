package com.example.genz.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String bank;

    private String description;

    public WithdrawRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public WithdrawRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }
}
