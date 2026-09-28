package com.example.genz.goal.controller;

import com.example.genz.goal.service.GoalService;
import com.example.genz.goal.dto.CreateGoalRequest;
import com.example.genz.goal.dto.GoalResponse;
import com.example.genz.goal.dto.UpdateGoalRequest;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.common.api.ApiResponse;

import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.service.TransactionService;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final TransactionService transactionService;

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
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getMyGoals(Authentication authentication,@RequestParam(required = false) GoalStatus status) {
        String phoneNumber = authentication.getName();
        List<GoalResponse> goals = goalService.getMyGoals(phoneNumber, status);
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

    @GetMapping("/{goalId}/transactions")
    public ResponseEntity<ApiResponse<TransactionPageResponse<TransactionResponse>>> getGoalTransactions(
            @PathVariable UUID goalId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        TransactionPageResponse<TransactionResponse> response = transactionService.getGoalTransactions(
                goalId, type, page, size, phoneNumber
        );
        return ResponseEntity.ok(ApiResponse.success("Goal transactions retrieved successfully", response));
    }

    @GetMapping("/{goalId}/history")
    public ResponseEntity<ApiResponse<TransactionPageResponse<TransactionResponse>>> getGoalHistory(
            @PathVariable UUID goalId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        return getGoalTransactions(goalId, type, page, size, authentication);
    }
}
