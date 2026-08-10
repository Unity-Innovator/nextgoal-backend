package com.example.genz.Service.GoalService;

import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.UpdateGoalRequest;

import java.util.List;
import java.util.UUID;

public interface GoalService {
    GoalResponse createGoal(CreateGoalRequest request, String phoneNumber);
    List<GoalResponse> getMyGoals(String phoneNumber);
    GoalResponse getGoal(UUID goalId, String phoneNumber);
    GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, String phoneNumber);
    void deleteGoal(UUID goalId, String phoneNumber);
}
