package com.example.genz.learning.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndUser(UUID id, User user);

    Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

    List<QuizAttempt> findByUserOrderByStartedAtDesc(User user);
}
