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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/withdrawal-requests")
@RequiredArgsConstructor
public class WithdrawalRequestController {

    private final WithdrawalRequestService withdrawalRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> createWithdrawalRequest(
            @Valid @RequestBody CreateWithdrawalRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.createRequest(request.getGoalId(), request, phoneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal request created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WithdrawalRequestResponse>>> getWithdrawalRequests(
            @RequestParam(required = false) UUID goalId,
            @RequestParam(required = false) WithdrawalStatus status,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        List<WithdrawalRequestResponse> responses = withdrawalRequestService.getRequests(goalId, status, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal requests retrieved successfully", responses));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> getWithdrawalRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.getRequestById(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request retrieved successfully", response));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> approveWithdrawalRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.approveRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request approved successfully", response));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> rejectWithdrawalRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.rejectRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request rejected successfully", response));
    }

    @RequireStepUp
    @PostMapping("/{requestId}/confirm")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> confirmWithdrawal(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.confirmWithdrawal(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal confirmed successfully", response));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> cancelWithdrawalRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawalRequestResponse response = withdrawalRequestService.cancelRequest(requestId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request cancelled successfully", response));
    }
}
