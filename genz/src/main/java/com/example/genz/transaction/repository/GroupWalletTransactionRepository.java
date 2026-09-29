package com.example.genz.transaction.repository;

import com.example.genz.transaction.entity.GroupWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupWalletTransactionRepository extends JpaRepository<GroupWalletTransaction, UUID> {

    List<GroupWalletTransaction> findByWithdrawalRequestId(UUID requestId);

    List<GroupWalletTransaction> findByGroupWalletIdOrderByCreatedAtDesc(UUID groupWalletId);
}
