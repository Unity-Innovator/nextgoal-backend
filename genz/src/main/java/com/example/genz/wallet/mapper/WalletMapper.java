package com.example.genz.wallet.mapper;

import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.dto.WalletResponse;
import com.example.genz.wallet.entity.Wallet;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.TransactionDirection;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletMapper {

    public WalletResponse toWalletResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO)
                .currency(wallet.getCurrency() != null ? wallet.getCurrency().name() : "USD")
                .build();
    }

    public DepositResponse toDepositResponse(boolean success, String message, BigDecimal newBalance) {
        return new DepositResponse(success, message, newBalance);
    }

    public DepositResponse toDepositResponse(
            boolean success,
            String message,
            BigDecimal newBalance,
            Integer coinsEarned,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            Double progressPercentage,
            com.example.genz.goal.enums.GoalStatus goalStatus
    ) {
        DepositResponse.DepositData data = DepositResponse.DepositData.builder()
                .currentAmount(currentAmount)
                .targetAmount(targetAmount)
                .progressPercentage(progressPercentage)
                .goalStatus(goalStatus)
                .coinsEarned(coinsEarned)
                .balance(newBalance)
                .build();

        return DepositResponse.builder()
                .success(success)
                .message(message)
                .balance(newBalance)
                .coinsEarned(coinsEarned)
                .currentAmount(currentAmount)
                .targetAmount(targetAmount)
                .progressPercentage(progressPercentage)
                .goalStatus(goalStatus)
                .data(data)
                .build();
    }

    public GrouplWallet toGoalWalletEntity(User user, Goal goal, Currency currency) {
        GrouplWallet wallet = new GrouplWallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);
        wallet.setGoal(goal);
        wallet.setCurrency(currency);
        return wallet;
    }

    public WalletTransaction toWalletTransactionEntity(
            Wallet wallet,
            User user,
            Goal goal,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            TransactionType type,
            TransactionDirection direction,
            TransactionStatus status,
            String description
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setGoal(goal);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setType(type);
        transaction.setDirection(direction != null ? direction : (type != null ? type.getDefaultDirection() : TransactionDirection.OUT));
        transaction.setStatus(status);
        transaction.setDescription(description);
        return transaction;
    }

    public WalletTransaction toWalletTransactionEntity(
            Wallet wallet,
            User user,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            String description
    ) {
        BigDecimal currentBalance = wallet != null && wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        TransactionDirection direction = type != null ? type.getDefaultDirection() : TransactionDirection.OUT;
        return toWalletTransactionEntity(
                wallet,
                user,
                null,
                amount,
                currentBalance,
                currentBalance,
                type,
                direction,
                status,
                description
        );
    }

    public GoalContribution toGoalContributionEntity(Goal goal, GoalMember member, BigDecimal amount, String note) {
        GoalContribution contribution = new GoalContribution();
        contribution.setGoal(goal);
        contribution.setGoalMember(member);
        contribution.setAmount(amount);
        contribution.setNote(note);
        return contribution;
    }
}
