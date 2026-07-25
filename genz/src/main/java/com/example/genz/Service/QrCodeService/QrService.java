package com.example.genz.Service.QrCodeService;

import com.example.genz.Entity.OTP.User;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Exception.ResourceNoteFoundException;
import com.example.genz.DTO.QrCodeDTO.GoalWalletRequest;
import com.example.genz.DTO.QrCodeDTO.WalletRequest;
import com.example.genz.Entity.QrCode.GoalWallet;
import com.example.genz.Entity.QrCode.Wallet;
import com.example.genz.Enums.Currency;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.Repository.QrCodeRepository.WalletRepository;

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
    @Autowired
    private GoalWalletRepository goalWalletRepository;

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

    @Transactional(readOnly = true)
    public String generateGoalWalletQR(
            Authentication authentication, GoalWalletRequest goalWalletRequest
            ) {
        String username = authentication.getName();
        User user = userRepository.findByPhoneNumber(username)
                .orElseThrow(()->new ResourceNoteFoundException("User not found"));

        UUID goalId;
        try {
            goalId = UUID.fromString(goalWalletRequest.getGoalId());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResourceNoteFoundException("Invalid goalId: " + goalWalletRequest.getGoalId());
        }

        GoalWallet goalWallet = goalWalletRepository.findByCreatorIdAndGoalId(user.getId(), goalId)
                .orElseThrow(()->new ResourceNoteFoundException("Goal wallet not found for goalId: " + goalId));

        String transactionId = UUID.randomUUID().toString();
        String goalNumber = goalWallet.getGoalNumber();

        return  "UserName="+username+"|"+
            "goalNumber=" + goalNumber + "|" +
            "goalId=" + goalId + "|"+
            "txId=" + transactionId;
    }
}

