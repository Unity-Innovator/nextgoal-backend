package com.example.genz.dto.GoalDTO;

import com.example.genz.enums.ContributionFrequency;
import com.example.genz.enums.Currency;
import com.example.genz.enums.GoalCategory;
import com.example.genz.enums.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateGoalRequest {

    @NotBlank(message = "Goal name is required")
    @Size(min = 3, max = 100, message = "Goal name must be between 3 and 100 characters")
    private String goalName;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @FutureOrPresent(message = "Target date cannot be in the past")
    private LocalDate targetDate;

    @NotNull(message = "Category is required")
    private GoalCategory category;

    @NotNull(message = "Currency is required")
    private Currency currency;

    private GoalType goalType;

    @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    private BigDecimal contributionAmount;

    private ContributionFrequency frequency;

    @FutureOrPresent(message = "Next contribution date cannot be in the past")
    private LocalDate nextContributionDate;

    @FutureOrPresent(message = "First due date cannot be in the past")
    private LocalDate firstDueDate;

    private String imageUrl;
}
