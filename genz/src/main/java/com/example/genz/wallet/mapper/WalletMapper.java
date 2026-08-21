package com.example.genz.wallet.mapper;

import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletMapper {

    public DepositResponse toDepositResponse(boolean success, String message, BigDecimal newBalance) {
        return new DepositResponse(success, message, newBalance);
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
            GrouplWallet wallet,
            User user,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            String description
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setDescription(description);
        return transaction;
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
