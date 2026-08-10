package com.example.genz.DTO.GoalDTO;

import com.example.genz.Enums.GoalCategory;
import com.example.genz.Enums.GoalStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GoalResponse {
    private UUID goalId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private GoalCategory category;
    private Boolean autoSave;
    private String imageUrl;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
