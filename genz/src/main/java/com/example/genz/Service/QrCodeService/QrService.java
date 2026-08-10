package com.example.genz.Service.QrCodeService;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.DTO.QrCodeDTO.GoalWalletRequest;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.exception.ResourceNoteFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrService {

    private final UserRepository userRepository;
    private final GoalWalletRepository goalWalletRepository;

    @Transactional(readOnly = true)
    public String generateGoalWalletQR(
            Authentication authentication, GoalWalletRequest goalWalletRequest
            ) {
        String username = authentication.getName();
        User user = userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        UUID goalId;
        try {
            goalId = UUID.fromString(goalWalletRequest.getGoalId());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResourceNoteFoundException("Invalid goalId: " + goalWalletRequest.getGoalId());
        }

        GrouplWallet goalWallet = goalWalletRepository.findByUserIdAndGoalId(user.getId(), goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal wallet not found for goalId: " + goalId));

        String transactionId = UUID.randomUUID().toString();

        return "UserName=" + username + "|" +
                "walletId=" + goalWallet.getId() + "|" +
                "goalId=" + goalId + "|" +
                "goalName=" + goalWallet.getGoal().getGoalName() + "|" +
                "balance=" + goalWallet.getBalance() + "|" +
                "txId=" + transactionId;
    }
}
