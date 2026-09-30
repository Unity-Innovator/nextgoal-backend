package com.example.genz.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpOptionsResponse {
    private List<BankOption> banks;
    private List<String> currencies;
    private List<BigDecimal> quickAmounts;
    private List<PaymentMethodOption> paymentMethods;
    private Double exchangeRateKhrToUsd;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankOption {
        private String code;
        private String name;
        private String logoUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodOption {
        private String code;
        private String name;
        private String icon;
    }
}
