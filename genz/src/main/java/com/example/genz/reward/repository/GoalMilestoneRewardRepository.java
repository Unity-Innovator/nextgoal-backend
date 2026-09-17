package com.example.genz.reward.repository;

import com.example.genz.reward.entity.GoalMilestoneReward;
import com.example.genz.reward.enums.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalMilestoneRewardRepository extends JpaRepository<GoalMilestoneReward, UUID> {

    boolean existsByGoalIdAndMilestone(UUID goalId, GoalMilestone milestone);

    boolean existsByGoalIdAndMilestoneAndUserId(UUID goalId, GoalMilestone milestone, UUID userId);

    List<GoalMilestoneReward> findByGoalId(UUID goalId);

    List<GoalMilestoneReward> findByGoalIdAndMilestone(UUID goalId, GoalMilestone milestone);

    List<GoalMilestoneReward> findByUserId(UUID userId);
}
