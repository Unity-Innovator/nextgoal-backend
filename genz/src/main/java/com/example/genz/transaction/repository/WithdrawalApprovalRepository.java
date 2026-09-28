package com.example.genz.transaction.repository;

import com.example.genz.transaction.entity.WithdrawalApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalApprovalRepository extends JpaRepository<WithdrawalApproval, UUID> {

    List<WithdrawalApproval> findByWithdrawalRequestId(UUID requestId);

    Optional<WithdrawalApproval> findByWithdrawalRequestIdAndUserId(UUID requestId, UUID userId);

    boolean existsByWithdrawalRequestIdAndUserId(UUID requestId, UUID userId);
}
