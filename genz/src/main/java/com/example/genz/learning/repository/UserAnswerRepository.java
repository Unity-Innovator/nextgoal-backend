package com.example.genz.learning.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.learning.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, UUID> {

    List<UserAnswer> findByQuizAttempt(QuizAttempt quizAttempt);

    List<UserAnswer> findByQuizAttemptId(UUID quizAttemptId);

    List<UserAnswer> findByUser(User user);

    Optional<UserAnswer> findByQuizAttemptIdAndQuestionId(UUID quizAttemptId, UUID questionId);
}
