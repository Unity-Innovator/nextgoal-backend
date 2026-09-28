package com.example.genz.transaction.service;

import com.example.genz.transaction.dto.CreateWithdrawalRequest;
import com.example.genz.transaction.dto.WithdrawalRequestResponse;
import com.example.genz.transaction.enums.WithdrawalStatus;

import java.util.List;
import java.util.UUID;

public interface WithdrawalRequestService {

    WithdrawalRequestResponse createRequest(UUID goalId, CreateWithdrawalRequest request, String phoneNumber);

    List<WithdrawalRequestResponse> getRequests(UUID goalId, WithdrawalStatus status, String phoneNumber);

    WithdrawalRequestResponse getRequestById(UUID requestId, String phoneNumber);

    WithdrawalRequestResponse approveRequest(UUID requestId, String phoneNumber);

    WithdrawalRequestResponse rejectRequest(UUID requestId, String phoneNumber);

    WithdrawalRequestResponse cancelRequest(UUID requestId, String phoneNumber);

    WithdrawalRequestResponse confirmWithdrawal(UUID requestId, String phoneNumber);
}
