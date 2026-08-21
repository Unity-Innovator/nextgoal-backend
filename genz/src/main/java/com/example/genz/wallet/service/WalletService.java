package com.example.genz.wallet.service;

import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;

public interface WalletService {
    DepositResponse deposit(DepositRequest request, String phoneNumber);
}
