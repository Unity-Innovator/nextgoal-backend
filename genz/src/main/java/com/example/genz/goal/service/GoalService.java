package com.example.genz.goal.service;

import com.example.genz.goal.dto.ContributionPlanStatusResponse;
import com.example.genz.goal.dto.CreateGoalRequest;
import com.example.genz.goal.dto.GoalResponse;
import com.example.genz.goal.dto.SavingPlanResponse;
import com.example.genz.goal.dto.UpdateGoalRequest;
import com.example.genz.goal.dto.UpdateSavingPlanRequest;

import java.util.List;
import java.util.UUID;

public interface GoalService {
    GoalResponse createGoal(CreateGoalRequest request, String phoneNumber);
    List<GoalResponse> getMyGoals(String phoneNumber);
    GoalResponse getGoal(UUID goalId, String phoneNumber);
    GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, String phoneNumber);
    void deleteGoal(UUID goalId, String phoneNumber);
    SavingPlanResponse getSavingPlan(UUID goalId, String phoneNumber);
    SavingPlanResponse updateSavingPlan(UUID goalId, UpdateSavingPlanRequest request, String phoneNumber);
    ContributionPlanStatusResponse getContributionPlanStatus(UUID goalId, String phoneNumber);
}
