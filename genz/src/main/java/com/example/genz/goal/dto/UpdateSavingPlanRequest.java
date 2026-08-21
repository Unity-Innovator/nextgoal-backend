package com.example.genz.goal.dto;

import com.example.genz.goal.enums.ContributionFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateSavingPlanRequest {

    @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    private BigDecimal contributionAmount;

    private ContributionFrequency frequency;

    @FutureOrPresent(message = "Next contribution date cannot be in the past")
    private LocalDate nextContributionDate;
}
