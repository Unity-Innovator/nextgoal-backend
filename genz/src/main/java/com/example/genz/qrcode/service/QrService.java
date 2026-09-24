package com.example.genz.qrcode.service;

import com.example.genz.qrcode.dto.GoalWalletRequest;
import com.example.genz.auth.entity.User;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;

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
