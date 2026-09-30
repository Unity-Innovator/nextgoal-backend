package com.example.genz.wallet.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.security.annotation.RequireStepUp;
import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.service.TransactionService;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.dto.TopUpOptionsResponse;
import com.example.genz.wallet.dto.TopUpRequest;
import com.example.genz.wallet.dto.TopUpResponse;
import com.example.genz.wallet.dto.WalletResponse;
import com.example.genz.wallet.dto.WalletSummaryResponse;
import com.example.genz.wallet.dto.WithdrawRequest;
import com.example.genz.wallet.dto.WithdrawResponse;
import com.example.genz.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(Authentication authentication) {
        String phoneNumber = authentication.getName();
        WalletResponse response = walletService.getWalletByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WalletSummaryResponse>> getWalletSummary(Authentication authentication) {
        String phoneNumber = authentication.getName();
        WalletSummaryResponse response = walletService.getWalletSummary(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Wallet summary retrieved successfully", response));
    }

    @GetMapping("/top-up/options")
    public ResponseEntity<ApiResponse<TopUpOptionsResponse>> getTopUpOptions() {
        TopUpOptionsResponse response = walletService.getTopUpOptions();
        return ResponseEntity.ok(ApiResponse.success("Top up options retrieved successfully", response));
    }

    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<TopUpResponse>> topUp(
            @Valid @RequestBody TopUpRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        TopUpResponse response = walletService.topUp(request, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully", response));
    }

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

    @RequireStepUp
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WithdrawResponse>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        WithdrawResponse response = walletService.withdraw(request, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal processed successfully", response));
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
