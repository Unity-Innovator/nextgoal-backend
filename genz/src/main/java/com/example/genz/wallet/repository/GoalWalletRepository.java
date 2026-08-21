package com.example.genz.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.wallet.entity.GrouplWallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalWalletRepository extends JpaRepository<GrouplWallet, UUID> {
    Optional<GrouplWallet> findByUserIdAndGoalId(UUID userId, UUID goalId);
    List<GrouplWallet> findByUserId(UUID userId);
    Optional<GrouplWallet> findByGoal(com.example.genz.goal.entity.Goal goal);
    Optional<GrouplWallet> findByGoalId(UUID goalId);
}
