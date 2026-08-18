package com.example.genz.dto.GoalDTO;

import com.example.genz.enums.CompletionStatus;
import com.example.genz.enums.GoalCategory;
import com.example.genz.enums.GoalStatus;
import com.example.genz.enums.GoalType;
import com.example.genz.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResponse {
    private UUID goalId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private Integer progressPercentage;

    private LocalDate targetDate;
    private LocalDate expectedCompletionDate;
    private LocalDate estimatedCompletionDate;

    private CompletionStatus completionStatus;
    private Long daysDifference;

    private GoalCategory category;
    private GoalType goalType;
    private String imageUrl;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private OwnerInfo owner;
    private List<MemberInfo> members;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnerInfo {
        private UUID userId;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private UUID userId;
        private String name;
        private MemberRole role;
    }
}
