package com.example.genz.mapper;

import com.example.genz.dto.GoalDTO.ContributionPlanStatusResponse;
import com.example.genz.dto.GoalDTO.CreateGoalRequest;
import com.example.genz.dto.GoalDTO.SavingPlanResponse;
import com.example.genz.dto.GoalDTO.UpdateSavingPlanRequest;
import com.example.genz.entity.Goal.Goal;
import com.example.genz.entity.Goal.SavingPlan;
import com.example.genz.enums.PeriodStatus;
import com.example.genz.enums.SavingPlanStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SavingPlanMapper {

    public SavingPlan toEntity(CreateGoalRequest request, Goal goal, LocalDate initialDueDate) {
        if (request == null || request.getContributionAmount() == null || request.getFrequency() == null) {
            return null;
        }
        SavingPlan plan = new SavingPlan();
        plan.setGoal(goal);
        plan.setContributionAmount(request.getContributionAmount());
        plan.setFrequency(request.getFrequency());
        plan.setNextContributionDate(initialDueDate != null ? initialDueDate : LocalDate.now());
        plan.setStatus(SavingPlanStatus.ACTIVE);
        plan.setContributedThisPeriod(BigDecimal.ZERO);
        return plan;
    }

    public void updateEntity(SavingPlan plan, UpdateSavingPlanRequest request) {
        if (plan == null || request == null) {
            return;
        }
        if (request.getContributionAmount() != null) {
            plan.setContributionAmount(request.getContributionAmount());
        }
        if (request.getFrequency() != null) {
            plan.setFrequency(request.getFrequency());
        }
        if (request.getNextContributionDate() != null) {
            plan.setNextContributionDate(request.getNextContributionDate());
        }
    }

    public SavingPlanResponse toSavingPlanResponse(SavingPlan plan, int progressPercentage, LocalDate estimatedCompletionDate) {
        if (plan == null) {
            return null;
        }
        return SavingPlanResponse.builder()
                .frequency(plan.getFrequency())
                .contributionAmount(plan.getContributionAmount())
                .estimatedCompletionDate(estimatedCompletionDate)
                .nextContributionDate(plan.getNextContributionDate())
                .status(plan.getStatus())
                .contributedThisPeriod(plan.getContributedThisPeriod())
                .progress(progressPercentage)
                .build();
    }

    public ContributionPlanStatusResponse toContributionPlanStatusResponse(
            Goal goal,
            SavingPlan plan,
            BigDecimal remainingAmount,
            BigDecimal remainingThisPeriod,
            PeriodStatus periodStatus,
            LocalDate estimatedCompletionDate
    ) {
        if (goal == null || plan == null) {
            return null;
        }
        BigDecimal contributedThisPeriod = plan.getContributedThisPeriod() != null ? plan.getContributedThisPeriod() : BigDecimal.ZERO;
        return ContributionPlanStatusResponse.builder()
                .goalId(goal.getId())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remainingAmount(remainingAmount)
                .contributionAmount(plan.getContributionAmount())
                .frequency(plan.getFrequency())
                .nextDueDate(plan.getNextContributionDate())
                .contributedThisPeriod(contributedThisPeriod)
                .remainingThisPeriod(remainingThisPeriod)
                .periodStatus(periodStatus)
                .planStatus(plan.getStatus())
                .estimatedCompletionDate(estimatedCompletionDate)
                .build();
    }
}
