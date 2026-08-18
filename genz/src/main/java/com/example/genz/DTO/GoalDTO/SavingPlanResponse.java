package com.example.genz.dto.GoalDTO;

import com.example.genz.enums.ContributionFrequency;
import com.example.genz.enums.SavingPlanStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SavingPlanResponse {
    private ContributionFrequency frequency;
    private BigDecimal contributionAmount;
    private LocalDate estimatedCompletionDate;
    private LocalDate nextContributionDate;
    private SavingPlanStatus status;
    private BigDecimal contributedThisPeriod;
    private int progress;
}
