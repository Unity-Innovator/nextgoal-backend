package com.example.genz.challenge.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ExpenseChallengeEvaluator implements ChallengeEvaluator {

    @Override
    public boolean supports(ChallengeType challengeType) {
        return challengeType == ChallengeType.LOG_EXPENSE || challengeType == ChallengeType.EXPENSE_LOG;
    }

    @Override
    public ChallengeEvaluationResult evaluate(User user, DailyChallenge challenge, LocalDate challengeDate, Object context) {
        // Expense logging entity/service is not currently implemented in this backend.
        // Return 0 progress and incomplete for forward compatibility.
        return ChallengeEvaluationResult.builder()
                .progress(BigDecimal.ZERO)
                .completed(false)
                .build();
    }
}
