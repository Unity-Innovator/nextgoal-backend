package com.example.genz.goal.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByOwnerOrderByCreatedAtDesc(User owner);

    @Query("SELECT g FROM Goal g WHERE (g.owner = :user OR EXISTS (SELECT 1 FROM GoalMember m WHERE m.goal = g AND m.user = :user AND m.status = com.example.genz.goal.enums.MemberStatus.ACTIVE)) ORDER BY g.createdAt DESC")
    List<Goal> findAccessibleGoals(@Param("user") User user);

    @Query("SELECT g FROM Goal g WHERE (g.owner = :user OR EXISTS (SELECT 1 FROM GoalMember m WHERE m.goal = g AND m.user = :user AND m.status = com.example.genz.goal.enums.MemberStatus.ACTIVE)) AND g.status = :status ORDER BY g.createdAt DESC")
    List<Goal> findAccessibleGoalsByStatus(@Param("user") User user, @Param("status") GoalStatus status);

    Optional<Goal> findByIdAndOwner(UUID id, User owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select goal from Goal goal where goal.id = :goalId")
    Optional<Goal> findByIdForUpdate(@Param("goalId") UUID goalId);

    boolean existsByGoalNameAndOwner(String goalName, User owner);

    long countByOwner(User owner);

    long countByOwnerAndStatus(User owner, GoalStatus status);

    @Query("SELECT COUNT(g) FROM Goal g WHERE g.owner.id = :ownerId AND g.createdAt >= :start AND g.createdAt <= :end")
    long countByOwnerIdAndCreatedAtBetween(
            @Param("ownerId") UUID ownerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
