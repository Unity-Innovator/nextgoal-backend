package com.example.genz.goal.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Goal> findByIdAndOwner(UUID id, User owner);

    boolean existsByGoalNameAndOwner(String goalName, User owner);

    long countByOwner(User owner);

    long countByOwnerAndStatus(User owner, GoalStatus status);
}
