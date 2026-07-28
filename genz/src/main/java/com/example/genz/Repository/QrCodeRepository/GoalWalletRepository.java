package com.example.genz.Repository.QrCodeRepository;

import com.example.genz.Entity.Wallet.GoalWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoalWalletRepository extends JpaRepository<GoalWallet, UUID> {
    Optional<GoalWallet> findByCreatorIdAndGoalId(UUID creatorId, UUID goalId);
}
