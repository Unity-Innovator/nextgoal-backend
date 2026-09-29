package com.example.genz.transaction.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.dto.TransactionUserResponse;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponse<TransactionResponse> getGoalTransactions(
            UUID goalId,
            String type,
            int page,
            int size,
            String phoneNumber
    ) {
        User user = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserIdAndStatus(goal.getId(), user.getId(), MemberStatus.ACTIVE);

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this goal");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Collection<TransactionType> types = resolveTypes(type);

        Page<WalletTransaction> pagedResult = (types == null)
                ? walletTransactionRepository.findByGoal_Id(goalId, pageable)
                : walletTransactionRepository.findByGoal_IdAndTypeIn(goalId, types, pageable);

        return TransactionPageResponse.from(pagedResult.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponse<TransactionResponse> getWalletTransactions(
            String type,
            int page,
            int size,
            String phoneNumber
    ) {
        User user = getUserByPhone(phoneNumber);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Collection<TransactionType> types = resolveTypes(type);

        Page<WalletTransaction> pagedResult = (types == null)
                ? walletTransactionRepository.findByUser_Id(user.getId(), pageable)
                : walletTransactionRepository.findByUser_IdAndTypeIn(user.getId(), types, pageable);

        return TransactionPageResponse.from(pagedResult.map(this::toResponse));
    }

    private User getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
    }

    private Collection<TransactionType> resolveTypes(String type) {
        if (type == null || type.isBlank() || type.equalsIgnoreCase("ALL")) {
            return null;
        }
        String normalized = type.trim().toUpperCase();
        if ("WITHDRAW".equals(normalized) || "WITHDRAWAL".equals(normalized)) {
            return List.of(TransactionType.GOAL_WITHDRAWAL, TransactionType.WITHDRAWAL);
        }
        if ("GOAL_WITHDRAWAL".equals(normalized)) {
            return List.of(TransactionType.GOAL_WITHDRAWAL);
        }
        if ("DEPOSIT".equals(normalized)) {
            return List.of(TransactionType.GOAL_DEPOSIT, TransactionType.DEPOSIT);
        }
        if ("GOAL_DEPOSIT".equals(normalized)) {
            return List.of(TransactionType.GOAL_DEPOSIT);
        }
        if ("TOP_UP".equals(normalized) || "TOPUP".equals(normalized)) {
            return List.of(TransactionType.TOP_UP);
        }
        try {
            return List.of(TransactionType.valueOf(normalized));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TransactionResponse toResponse(WalletTransaction t) {
        String typeStr = (t.getType() != null) ? t.getType().name() : "GOAL_DEPOSIT";

        TransactionUserResponse userResp = null;
        if (t.getUser() != null) {
            userResp = TransactionUserResponse.builder()
                    .id(t.getUser().getId())
                    .fullName(t.getUser().getFullName())
                    .build();
        }

        String currencyStr = (t.getWallet() != null && t.getWallet().getCurrency() != null)
                ? t.getWallet().getCurrency().name()
                : "USD";

        String statusStr = (t.getStatus() != null) ? t.getStatus().name() : "SUCCESS";
        String directionStr = (t.getDirection() != null)
                ? t.getDirection().name()
                : (t.getType() != null ? t.getType().getDefaultDirection().name() : "OUT");

        UUID goalId = t.getGoal() != null ? t.getGoal().getId() : null;
        String goalName = t.getGoal() != null ? t.getGoal().getGoalName() : null;

        return TransactionResponse.builder()
                .id(t.getId())
                .user(userResp)
                .type(typeStr)
                .amount(t.getAmount())
                .direction(directionStr)
                .goalId(goalId)
                .goalName(goalName)
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .currency(currencyStr)
                .status(statusStr)
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
