package com.example.genz.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.genz.goal.enums.GoalCategory;

@Getter
@Setter
public class UpdateGoalRequest {

    @Size(min = 3, max = 100, message = "Goal name must be between 3 and 100 characters")
    private String goalName;

    @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @FutureOrPresent(message = "Target date cannot be in the past")
    private LocalDate targetDate;

    private GoalCategory category;

    private Boolean autoSave;

    private String imageUrl;
}
