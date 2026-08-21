package com.example.genz.goal.dto;

import com.example.genz.goal.enums.ContributionFrequency;
import com.example.genz.transaction.enums.PeriodStatus;
import com.example.genz.goal.enums.SavingPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionPlanStatusResponse {
    private UUID goalId;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal contributionAmount;
    private ContributionFrequency frequency;
    private LocalDate nextDueDate;
    private BigDecimal contributedThisPeriod;
    private BigDecimal remainingThisPeriod;
    private PeriodStatus periodStatus;
    private SavingPlanStatus planStatus;
    private LocalDate estimatedCompletionDate;
}
