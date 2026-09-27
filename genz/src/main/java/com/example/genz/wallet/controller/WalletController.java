package com.example.genz.wallet.controller;

import com.example.genz.wallet.service.WalletService;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;





import com.example.genz.common.api.ApiResponse;
import com.example.genz.security.annotation.RequireStepUp;
import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @RequireStepUp
    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        DepositResponse response = walletService.deposit(request, phoneNumber);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<TransactionPageResponse<TransactionResponse>>> getWalletTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        TransactionPageResponse<TransactionResponse> response = transactionService.getWalletTransactions(
                type, page, size, phoneNumber
        );
        return ResponseEntity.ok(ApiResponse.success("Wallet transactions retrieved successfully", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<TransactionPageResponse<TransactionResponse>>> getWalletHistory(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        return getWalletTransactions(type, page, size, authentication);
    }
}
