package com.example.genz.wallet.service;

import com.example.genz.auth.entity.User;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.dto.TopUpOptionsResponse;
import com.example.genz.wallet.dto.TopUpRequest;
import com.example.genz.wallet.dto.TopUpResponse;
import com.example.genz.wallet.dto.WalletResponse;
import com.example.genz.wallet.dto.WalletSummaryResponse;
import com.example.genz.wallet.entity.Wallet;

public interface WalletService {
    DepositResponse deposit(DepositRequest request, String phoneNumber);

    WalletResponse getWalletByPhoneNumber(String phoneNumber);

    WalletSummaryResponse getWalletSummary(String phoneNumber);

    TopUpResponse topUp(TopUpRequest request, String phoneNumber);

    TopUpOptionsResponse getTopUpOptions();

    Wallet getOrCreateWallet(User user);
}
