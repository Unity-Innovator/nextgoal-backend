package com.example.genz.service.GoalService;

import com.example.genz.dto.GoalDTO.ContributionPlanStatusResponse;
import com.example.genz.dto.GoalDTO.CreateGoalRequest;
import com.example.genz.dto.GoalDTO.GoalResponse;
import com.example.genz.dto.GoalDTO.SavingPlanResponse;
import com.example.genz.dto.GoalDTO.UpdateGoalRequest;
import com.example.genz.dto.GoalDTO.UpdateSavingPlanRequest;

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
