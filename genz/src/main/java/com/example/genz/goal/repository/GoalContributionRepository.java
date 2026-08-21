package com.example.genz.goal.repository;

import com.example.genz.transaction.entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {
    List<GoalContribution> findByGoalIdOrderByContributedAtAsc(UUID goalId);

    @Query("SELECT COUNT(gc) FROM GoalContribution gc WHERE gc.goalMember.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(gc.amount), 0) FROM GoalContribution gc WHERE gc.goalMember.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") UUID userId);
}
