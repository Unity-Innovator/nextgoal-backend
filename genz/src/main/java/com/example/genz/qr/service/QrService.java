package com.example.genz.qr.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.exception.ResourceNoteFoundException;
import com.example.genz.qr.dto.WalletRequest;
import com.example.genz.qr.entity.Wallet;
import com.example.genz.qr.enums.Currency;
import com.example.genz.qr.repository.WalletRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QrService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public String generateFakeQR(
            Authentication authentication, WalletRequest walletRequest
            ) {
        String username = authentication.getName();
        User user = userRepository.findByPhoneNumber(username).orElseThrow(()->new ResourceNoteFoundException("User not found"));
if (user.getWallets() == null || user.getWallets().isEmpty()) {
    throw new ResourceNoteFoundException("User has no linked accounts: " + username);
}



Currency currency;
try {


    currency = Currency.valueOf(walletRequest.getCurrency().toUpperCase());
} catch (IllegalArgumentException | NullPointerException e) {
    throw new ResourceNoteFoundException("Invalid currency: " + walletRequest.getCurrency());
}

    Wallet wallet = walletRepository.findByUsersAndCurrency(user, currency)
        .orElseThrow(()->new ResourceNoteFoundException("Wallet not found for currency: " + currency));

    String transactionId = UUID.randomUUID().toString();
    String accountNumber = wallet.getAccountNumber();

    return  "UserName="+username+"|"+
        "accountNumber=" + accountNumber + "|" +
        "currency=" + currency + "|"+
        "txId=" + transactionId;
    }
}

