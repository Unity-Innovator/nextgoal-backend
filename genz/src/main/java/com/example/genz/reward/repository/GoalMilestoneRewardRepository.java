package com.example.genz.reward.repository;

import com.example.genz.reward.entity.GoalMilestoneReward;
import com.example.genz.reward.enums.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GoalMilestoneRewardRepository extends JpaRepository<GoalMilestoneReward, UUID> {

    boolean existsByGoalIdAndMilestone(UUID goalId, GoalMilestone milestone);

    boolean existsByGoalIdAndMilestoneAndUserId(UUID goalId, GoalMilestone milestone, UUID userId);

    List<GoalMilestoneReward> findByGoalId(UUID goalId);

    List<GoalMilestoneReward> findByGoalIdAndMilestone(UUID goalId, GoalMilestone milestone);

    List<GoalMilestoneReward> findByUserId(UUID userId);

    @Query("SELECT COUNT(gmr) FROM GoalMilestoneReward gmr WHERE gmr.user.id = :userId AND gmr.createdAt >= :start AND gmr.createdAt <= :end")
    long countByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(gmr) FROM GoalMilestoneReward gmr WHERE gmr.user.id = :userId AND gmr.milestone = :milestone AND gmr.createdAt >= :start AND gmr.createdAt <= :end")
    long countByUserIdAndMilestoneAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("milestone") GoalMilestone milestone,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
