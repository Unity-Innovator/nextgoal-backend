package com.example.genz.challenge.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.learning.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class QuizChallengeEvaluator implements ChallengeEvaluator {

    private final QuizAttemptRepository quizAttemptRepository;

    @Override
    public boolean supports(ChallengeType challengeType) {
        return challengeType == ChallengeType.COMPLETE_QUIZ || challengeType == ChallengeType.QUIZ;
    }

    @Override
    public ChallengeEvaluationResult evaluate(User user, DailyChallenge challenge, LocalDate challengeDate, Object context) {
        LocalDateTime startOfDay = challengeDate.atStartOfDay();
        LocalDateTime endOfDay = challengeDate.atTime(LocalTime.MAX);

        int target = 1;
        if (challenge.getTargetValue() != null) {
            target = challenge.getTargetValue().intValue();
        } else if (challenge.getRule() != null && challenge.getRule().getTargetCount() != null) {
            target = challenge.getRule().getTargetCount();
        }

        long count = quizAttemptRepository.countByUserIdAndIsCompletedTrueAndCompletedAtBetween(user.getId(), startOfDay, endOfDay);
        if (count == 0 && context instanceof com.example.genz.learning.entity.QuizAttempt qa && Boolean.TRUE.equals(qa.getIsCompleted())) {
            count = 1;
        }
        BigDecimal progress = BigDecimal.valueOf(count);

        return ChallengeEvaluationResult.builder()
                .progress(progress)
                .completed(count >= target)
                .build();
    }
}
