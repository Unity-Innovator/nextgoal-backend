package com.example.genz.learning.mapper;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.dto.*;
import com.example.genz.learning.entity.LessonQuestion;
import com.example.genz.learning.entity.QuizAttempt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuizMapper {

    public QuizQuestionResponse toQuizQuestionResponse(LessonQuestion question) {
        if (question == null) {
            return null;
        }
        return QuizQuestionResponse.builder()
                .id(question.getId())
                .lessonId(question.getLesson() != null ? question.getLesson().getId() : null)
                .question(question.getQuestion())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .orderIndex(question.getOrderIndex())
                .build();
    }

    public List<QuizQuestionResponse> toQuizQuestionResponseList(List<LessonQuestion> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .map(this::toQuizQuestionResponse)
                .collect(Collectors.toList());
    }

    public QuizStartResponse toQuizStartResponse(QuizAttempt attempt, List<LessonQuestion> questions, Integer remainingEnergy) {
        return QuizStartResponse.builder()
                .quizAttemptId(attempt.getId())
                .totalQuestions(attempt.getTotalQuestions())
                .questions(toQuizQuestionResponseList(questions))
                .remainingEnergy(remainingEnergy)
                .build();
    }

    public QuizResultResponse toQuizResultResponse(QuizAttempt attempt, Integer remainingEnergy) {
        if (attempt == null) {
            return null;
        }
        int scorePct = (attempt.getScore() != null) ? attempt.getScore().intValue() : 0;
        return QuizResultResponse.builder()
                .quizAttemptId(attempt.getId())
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .scorePercentage(scorePct)
                .baseXp(attempt.getBaseXp())
                .bonusXp(attempt.getBonusXp())
                .totalXpEarned(attempt.getTotalXp())
                .remainingEnergy(remainingEnergy)
                .completedAt(attempt.getCompletedAt())
                .build();
    }

    public QuizUserEnergyResponse toQuizUserEnergyResponse(User user) {
        if (user == null) {
            return null;
        }
        int currentEnergy = user.getEnergy() != null ? user.getEnergy() : 100;
        int currentXp = user.getTotalXp() != null ? user.getTotalXp() : 0;
        return QuizUserEnergyResponse.builder()
                .energy(currentEnergy)
                .maxEnergy(100)
                .energyResetDate(user.getEnergyResetDate())
                .totalXp(currentXp)
                .build();
    }
}
