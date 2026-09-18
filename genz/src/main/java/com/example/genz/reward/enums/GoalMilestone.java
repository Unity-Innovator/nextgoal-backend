package com.example.genz.reward.enums;

import java.math.BigDecimal;

public enum GoalMilestone {
    MILESTONE_25(new BigDecimal("25"), new BigDecimal("0.25")),
    MILESTONE_50(new BigDecimal("50"), new BigDecimal("0.50")),
    MILESTONE_75(new BigDecimal("75"), new BigDecimal("0.75")),
    MILESTONE_100(new BigDecimal("100"), new BigDecimal("1.00"));

    private final BigDecimal percentage;
    private final BigDecimal fraction;

    GoalMilestone(BigDecimal percentage, BigDecimal fraction) {
        this.percentage = percentage;
        this.fraction = fraction;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public BigDecimal getFraction() {
        return fraction;
    }
}
