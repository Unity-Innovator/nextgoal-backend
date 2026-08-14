package com.example.genz.repository.GoalRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Goal.Goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Goal> findByIdAndOwner(UUID id, User owner);

    boolean existsByGoalNameAndOwner(String goalName, User owner);
}
