package com.example.genz.mapper;

import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.UpdateGoalRequest;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Enums.CompletionStatus;
import com.example.genz.Enums.GoalStatus;
import com.example.genz.Enums.GoalType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class GoalMapper {

    public Goal toEntity(CreateGoalRequest request, User owner, GoalType goalType) {
        if (request == null) {
            return null;
        }
        Goal goal = new Goal();
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setRemaining(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setGoalType(goalType != null ? goalType : GoalType.FLEXIBLE);
        goal.setCategory(request.getCategory());
        goal.setImageUrl(request.getImageUrl());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setOwner(owner);
        goal.setCreatedAt(LocalDateTime.now());
        return goal;
    }

    public void updateEntity(Goal goal, UpdateGoalRequest request) {
        if (goal == null || request == null) {
            return;
        }
        if (request.getGoalName() != null) {
            goal.setGoalName(request.getGoalName());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
            BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
            BigDecimal rem = request.getTargetAmount().subtract(current);
            goal.setRemaining(rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem);
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getCategory() != null) {
            goal.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            goal.setImageUrl(request.getImageUrl());
        }
    }

    public GoalResponse toGoalResponse(
            Goal goal,
            BigDecimal currentAmount,
            BigDecimal remainingAmount,
            Integer progressPercentage,
            LocalDate expectedCompletionDate,
            LocalDate estimatedCompletionDate,
            CompletionStatus completionStatus,
            Long daysDifference,
            List<GoalMember> activeMembers
    ) {
        if (goal == null) {
            return null;
        }

        GoalResponse.OwnerInfo ownerInfo = null;
        if (goal.getOwner() != null) {
            ownerInfo = GoalResponse.OwnerInfo.builder()
                    .userId(goal.getOwner().getId())
                    .name(goal.getOwner().getFullName())
                    .build();
        }

        List<GoalResponse.MemberInfo> memberInfos = (activeMembers != null) ? activeMembers.stream()
                .map(m -> GoalResponse.MemberInfo.builder()
                        .userId(m.getUser() != null ? m.getUser().getId() : null)
                        .name(m.getUser() != null ? m.getUser().getFullName() : null)
                        .role(m.getRole())
                        .build())
                .toList() : Collections.emptyList();

        BigDecimal target = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;

        return GoalResponse.builder()
                .goalId(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(target)
                .currentAmount(currentAmount)
                .remainingAmount(remainingAmount)
                .progressPercentage(progressPercentage)
                .targetDate(goal.getTargetDate())
                .expectedCompletionDate(expectedCompletionDate)
                .estimatedCompletionDate(estimatedCompletionDate)
                .completionStatus(completionStatus)
                .daysDifference(daysDifference)
                .category(goal.getCategory())
                .goalType(goal.getGoalType() != null ? goal.getGoalType() : GoalType.FLEXIBLE)
                .imageUrl(goal.getImageUrl())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .completedAt(goal.getCompletedAt())
                .owner(ownerInfo)
                .members(memberInfos)
                .build();
    }
}
