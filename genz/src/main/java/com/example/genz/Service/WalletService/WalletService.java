package com.example.genz.service.WalletService;

import com.example.genz.dto.WalletDTO.DepositRequest;
import com.example.genz.dto.WalletDTO.DepositResponse;

public interface WalletService {
    DepositResponse deposit(DepositRequest request, String phoneNumber);
}
