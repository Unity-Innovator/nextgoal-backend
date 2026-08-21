package com.example.genz.goal.repository;

import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GoalMemberRepository extends JpaRepository<GoalMember, UUID> {

    List<GoalMember> findByGoalAndStatus(Goal goal, MemberStatus status);

    @Query("SELECT m FROM GoalMember m JOIN FETCH m.user WHERE m.goal = :goal AND m.status = :status")
    List<GoalMember> findByGoalAndStatusWithUser(@Param("goal") Goal goal, @Param("status") MemberStatus status);

    long countByGoalAndStatus(Goal goal, MemberStatus status);

    boolean existsByGoalIdAndUserId(UUID goalId, UUID userId);

    boolean existsByGoalIdAndUserIdAndStatus(UUID goalId, UUID userId, MemberStatus status);

    java.util.Optional<GoalMember> findByGoalIdAndUserId(UUID goalId, UUID userId);
}
