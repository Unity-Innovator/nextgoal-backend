package com.example.genz.transaction.repository;

import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    @Query("SELECT DISTINCT wt.createdAt FROM WalletTransaction wt WHERE wt.user.id = :userId AND wt.type IN (com.example.genz.transaction.enums.TransactionType.DEPOSIT, com.example.genz.transaction.enums.TransactionType.GOAL_DEPOSIT) AND wt.status = com.example.genz.transaction.enums.TransactionStatus.SUCCESS")
    List<LocalDateTime> findDistinctDepositCreatedAtByUserId(@Param("userId") UUID userId);

    Page<WalletTransaction> findByGoal_Id(UUID goalId, Pageable pageable);

    Page<WalletTransaction> findByGoal_IdAndTypeIn(UUID goalId, Collection<TransactionType> types, Pageable pageable);

    Page<WalletTransaction> findByUser_Id(UUID userId, Pageable pageable);

    Page<WalletTransaction> findByUser_IdAndTypeIn(UUID userId, Collection<TransactionType> types, Pageable pageable);

    Page<WalletTransaction> findByWallet_Id(UUID walletId, Pageable pageable);

    Page<WalletTransaction> findByWallet_IdAndTypeIn(UUID walletId, Collection<TransactionType> types, Pageable pageable);
}
