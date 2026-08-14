package com.example.genz.service.GoalService;

import java.util.List;
import java.util.UUID;

import com.example.genz.dto.GoalDTO.CreateGoalRequest;
import com.example.genz.dto.GoalDTO.GoalResponse;
import com.example.genz.dto.GoalDTO.UpdateGoalRequest;

public interface GoalService {
    GoalResponse createGoal(CreateGoalRequest request, String phoneNumber);
    List<GoalResponse> getMyGoals(String phoneNumber);
    GoalResponse getGoal(UUID goalId, String phoneNumber);
    GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, String phoneNumber);
    void deleteGoal(UUID goalId, String phoneNumber);
}
