package com.example.genz.learning.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.entity.LessonQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonQuestionRepository extends JpaRepository<LessonQuestion, UUID> {

    @Query("SELECT q FROM LessonQuestion q WHERE (q.lesson.isActive = true OR q.lesson.isActive IS NULL) AND q.id NOT IN (SELECT ua.question.id FROM UserAnswer ua WHERE ua.user = :user)")
    List<LessonQuestion> findActiveQuestionsUnansweredByUser(@Param("user") User user);

    @Query("SELECT q FROM LessonQuestion q WHERE (q.lesson.isActive = true OR q.lesson.isActive IS NULL)")
    List<LessonQuestion> findAllActiveQuestions();

    @Query("SELECT q FROM LessonQuestion q WHERE (q.lesson.isActive = true OR q.lesson.isActive IS NULL) ORDER BY (SELECT COALESCE(MAX(ua.answeredAt), '1970-01-01 00:00:00') FROM UserAnswer ua WHERE ua.question = q AND ua.user = :user) ASC")
    List<LessonQuestion> findActiveQuestionsLeastRecentlyAnsweredByUser(@Param("user") User user);
}
