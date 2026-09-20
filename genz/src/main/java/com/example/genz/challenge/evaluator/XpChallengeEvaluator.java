package com.example.genz.challenge.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.reward.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class XpChallengeEvaluator implements ChallengeEvaluator {

    private final XpTransactionRepository xpTransactionRepository;

    @Override
    public boolean supports(ChallengeType challengeType) {
        return challengeType == ChallengeType.EARN_XP;
    }

    @Override
    public ChallengeEvaluationResult evaluate(User user, DailyChallenge challenge, LocalDate challengeDate, Object context) {
        LocalDateTime startOfDay = challengeDate.atStartOfDay();
        LocalDateTime endOfDay = challengeDate.atTime(LocalTime.MAX);

        int target = 50;
        if (challenge.getTargetValue() != null) {
            target = challenge.getTargetValue().intValue();
        }

        int todayXp = xpTransactionRepository.sumPointsByUserIdAndCreatedAtBetweenExcludingType(
                user.getId(),
                startOfDay,
                endOfDay,
                ActivityType.DAILY_CHALLENGE
        );

        BigDecimal progress = BigDecimal.valueOf(todayXp);

        return ChallengeEvaluationResult.builder()
                .progress(progress)
                .completed(todayXp >= target)
                .build();
    }
}
