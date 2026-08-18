package com.example.genz.repository.QrCodeRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.entity.Wallet.GrouplWallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalWalletRepository extends JpaRepository<GrouplWallet, UUID> {
    Optional<GrouplWallet> findByUserIdAndGoalId(UUID userId, UUID goalId);
    List<GrouplWallet> findByUserId(UUID userId);
    Optional<GrouplWallet> findByGoal(com.example.genz.entity.Goal.Goal goal);
    Optional<GrouplWallet> findByGoalId(UUID goalId);
}
