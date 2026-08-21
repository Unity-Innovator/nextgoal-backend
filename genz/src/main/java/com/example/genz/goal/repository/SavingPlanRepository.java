package com.example.genz.goal.repository;

import com.example.genz.goal.entity.SavingPlan;
import com.example.genz.goal.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingPlanRepository extends JpaRepository<SavingPlan, UUID> {

    Optional<SavingPlan> findByGoalId(UUID goalId);

    List<SavingPlan> findByNextContributionDateLessThanEqualAndGoal_Status(LocalDate date, GoalStatus status);
}
