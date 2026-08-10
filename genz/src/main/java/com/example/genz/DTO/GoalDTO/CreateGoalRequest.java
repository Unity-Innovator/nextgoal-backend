package com.example.genz.DTO.GoalDTO;

import com.example.genz.Enums.Currency;
import com.example.genz.Enums.GoalCategory;
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

    @NotNull(message = "Target date is required")
    @FutureOrPresent(message = "Target date cannot be in the past")
    private LocalDate targetDate;

    @NotNull(message = "Category is required")
    private GoalCategory category;

    @NotNull(message = "Currency is required")
    private Currency currency;

    private String imageUrl;
}
