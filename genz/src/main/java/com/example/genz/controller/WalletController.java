package com.example.genz.controller;

import com.example.genz.service.WalletService.WalletService;
import com.example.genz.dto.WalletDTO.DepositRequest;
import com.example.genz.dto.WalletDTO.DepositResponse;





import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        DepositResponse response = walletService.deposit(request, phoneNumber);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
