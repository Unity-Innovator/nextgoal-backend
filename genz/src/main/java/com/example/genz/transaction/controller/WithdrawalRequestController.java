package com.example.genz.transaction.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.security.annotation.RequireStepUp;
import com.example.genz.transaction.dto.CreateWithdrawalRequest;
import com.example.genz.transaction.dto.WithdrawalRequestResponse;
import com.example.genz.transaction.enums.WithdrawalStatus;
import com.example.genz.transaction.service.WithdrawalRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WithdrawalRequestController {

    private final WithdrawalRequestService withdrawalRequestService;

    @PostMapping({"/api/withdrawal-requests", "/api/goals/{goalId}/withdraw-requests"})
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> createWithdrawalRequest(
            @PathVariable(required = false) UUID goalId,
            @Valid @RequestBody CreateWithdrawalRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.createRequest(goalId, request, phoneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal request created successfully", response));
    }

    @GetMapping({"/api/withdrawal-requests", "/api/goals/{goalId}/withdraw-requests", "/api/goals/withdraw-requests"})
    public ResponseEntity<ApiResponse<List<WithdrawalRequestResponse>>> getWithdrawalRequests(
            @PathVariable(required = false) UUID goalId,
            @RequestParam(required = false) UUID goal,
            @RequestParam(required = false) WithdrawalStatus status,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        UUID effectiveGoalId = goalId != null ? goalId : goal;
        List<WithdrawalRequestResponse> responses = withdrawalRequestService.getRequests(effectiveGoalId, status, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal requests retrieved successfully", responses));
    }

    @GetMapping({
            "/api/withdrawal-requests/{requestId}",
            "/api/goals/{goalId}/withdraw-requests/{requestId}",
            "/api/goals/withdraw-requests/{requestId}"
    })
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> getWithdrawalRequest(
            @PathVariable(required = false) UUID goalId,
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.getRequestById(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request retrieved successfully", response));
    }

    @RequestMapping(
            value = {
                    "/api/withdrawal-requests/{requestId}/approve",
                    "/api/goals/withdraw-requests/{requestId}/approve",
                    "/api/goals/{goalId}/withdraw-requests/{requestId}/approve"
            },
            method = {RequestMethod.PUT, RequestMethod.POST}
    )
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> approveWithdrawalRequest(
            @PathVariable(required = false) UUID goalId,
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.approveRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request approved successfully", response));
    }

    @RequestMapping(
            value = {
                    "/api/withdrawal-requests/{requestId}/reject",
                    "/api/goals/withdraw-requests/{requestId}/reject",
                    "/api/goals/{goalId}/withdraw-requests/{requestId}/reject"
            },
            method = {RequestMethod.PUT, RequestMethod.POST}
    )
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> rejectWithdrawalRequest(
            @PathVariable(required = false) UUID goalId,
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.rejectRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request rejected successfully", response));
    }

    @RequireStepUp
    @PostMapping({
            "/api/withdrawal-requests/{requestId}/confirm",
            "/api/goals/withdraw-requests/{requestId}/confirm",
            "/api/goals/{goalId}/withdraw-requests/{requestId}/confirm"
    })
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> confirmWithdrawal(
            @PathVariable(required = false) UUID goalId,
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.confirmWithdrawal(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal confirmed successfully", response));
    }

    @PostMapping({
            "/api/withdrawal-requests/{requestId}/cancel",
            "/api/goals/withdraw-requests/{requestId}/cancel",
            "/api/goals/{goalId}/withdraw-requests/{requestId}/cancel"
    })
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> cancelWithdrawalRequest(
            @PathVariable(required = false) UUID goalId,
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.cancelRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request cancelled successfully", response));
    }
}
