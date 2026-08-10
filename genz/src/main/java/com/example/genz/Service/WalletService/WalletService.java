package com.example.genz.Service.WalletService;

import com.example.genz.DTO.WalletDTO.DepositRequest;
import com.example.genz.DTO.WalletDTO.DepositResponse;

public interface WalletService {
    DepositResponse deposit(DepositRequest request, String phoneNumber);
}
