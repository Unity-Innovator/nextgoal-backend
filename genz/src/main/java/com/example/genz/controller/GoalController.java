package com.example.genz.controller;

import com.example.genz.service.GoalService.GoalService;
import com.example.genz.dto.GoalDTO.CreateGoalRequest;
import com.example.genz.dto.GoalDTO.GoalResponse;
import com.example.genz.dto.GoalDTO.UpdateGoalRequest;
import com.example.genz.exception.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        GoalResponse response = goalService.createGoal(request, phoneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Goal created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getMyGoals(Authentication authentication) {
        String phoneNumber = authentication.getName();
        List<GoalResponse> goals = goalService.getMyGoals(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Goals retrieved successfully", goals));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoal(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        GoalResponse response = goalService.getGoal(goalId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Goal retrieved successfully", response));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        GoalResponse response = goalService.updateGoal(goalId, request, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Goal updated successfully", response));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        goalService.deleteGoal(goalId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Goal cancelled successfully", null));
    }
}
