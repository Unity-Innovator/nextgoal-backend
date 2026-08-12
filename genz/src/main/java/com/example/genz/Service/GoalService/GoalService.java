package com.example.genz.Service.GoalService;

import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.SavingPlanResponse;
import com.example.genz.DTO.GoalDTO.UpdateGoalRequest;
import com.example.genz.DTO.GoalDTO.UpdateSavingPlanRequest;

import java.util.List;
import java.util.UUID;

import com.example.genz.DTO.GoalDTO.ContributionPlanStatusResponse;

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
