package com.example.genz.Repository.GoalRepository;

import com.example.genz.Entity.Transaction.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {
    List<GoalContribution> findByGoalIdOrderByContributedAtAsc(UUID goalId);
}
