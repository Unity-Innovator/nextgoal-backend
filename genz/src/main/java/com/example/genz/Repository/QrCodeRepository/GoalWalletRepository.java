package com.example.genz.Repository.QrCodeRepository;

import com.example.genz.Entity.Wallet.GrouplWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalWalletRepository extends JpaRepository<GrouplWallet, UUID> {
    Optional<GrouplWallet> findByUserIdAndGoalId(UUID userId, UUID goalId);
    List<GrouplWallet> findByUserId(UUID userId);
    Optional<GrouplWallet> findByGoal(com.example.genz.Entity.Goal.Goal goal);
    Optional<GrouplWallet> findByGoalId(UUID goalId);
}
