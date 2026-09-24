package com.example.genz.learning.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndUser(UUID id, User user);

    Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

    List<QuizAttempt> findByUserOrderByStartedAtDesc(User user);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.isCompleted = true AND ((qa.completedAt >= :start AND qa.completedAt <= :end) OR (qa.completedAt IS NULL AND qa.startedAt >= :start AND qa.startedAt <= :end))")
    long countByUserIdAndIsCompletedTrueAndCompletedAtBetween(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
