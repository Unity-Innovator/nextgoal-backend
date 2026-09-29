package com.example.genz.transaction.repository;

import com.example.genz.transaction.entity.WithdrawalRequest;
import com.example.genz.transaction.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {

    List<WithdrawalRequest> findByGoalIdOrderByCreatedAtDesc(UUID goalId);

    List<WithdrawalRequest> findByGoalIdAndStatusOrderByCreatedAtDesc(UUID goalId, WithdrawalStatus status);

    boolean existsByGoalIdAndStatusIn(UUID goalId, Collection<WithdrawalStatus> statuses);

    @Query("SELECT wr FROM WithdrawalRequest wr WHERE " +
           "(wr.goal.owner.id = :userId OR EXISTS (SELECT 1 FROM GoalMember gm WHERE gm.goal = wr.goal AND gm.user.id = :userId AND gm.status = com.example.genz.goal.enums.MemberStatus.ACTIVE)) " +
           "ORDER BY wr.createdAt DESC")
    List<WithdrawalRequest> findAccessibleRequests(@Param("userId") UUID userId);

    @Query("SELECT wr FROM WithdrawalRequest wr WHERE " +
           "(wr.goal.owner.id = :userId OR EXISTS (SELECT 1 FROM GoalMember gm WHERE gm.goal = wr.goal AND gm.user.id = :userId AND gm.status = com.example.genz.goal.enums.MemberStatus.ACTIVE)) " +
           "AND wr.status = :status " +
           "ORDER BY wr.createdAt DESC")
    List<WithdrawalRequest> findAccessibleRequestsByStatus(@Param("userId") UUID userId, @Param("status") WithdrawalStatus status);
}
