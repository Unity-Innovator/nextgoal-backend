package com.example.genz.transaction.service;

import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;

import java.util.UUID;

public interface TransactionService {
    TransactionPageResponse<TransactionResponse> getGoalTransactions(
            UUID goalId,
            String type,
            int page,
            int size,
            String phoneNumber
    );

    TransactionPageResponse<TransactionResponse> getWalletTransactions(
            String type,
            int page,
            int size,
            String phoneNumber
    );
}
