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
