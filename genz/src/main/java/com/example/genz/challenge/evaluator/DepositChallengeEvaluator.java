package com.example.genz.challenge.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.goal.repository.GoalContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DepositChallengeEvaluator implements ChallengeEvaluator {

    private final GoalContributionRepository goalContributionRepository;

    @Override
    public boolean supports(ChallengeType challengeType) {
        return challengeType == ChallengeType.DEPOSIT_AMOUNT
                || challengeType == ChallengeType.DEPOSIT_ANY
                || challengeType == ChallengeType.GOAL_DEPOSIT;
    }

    @Override
    public ChallengeEvaluationResult evaluate(User user, DailyChallenge challenge, LocalDate challengeDate, Object context) {
        LocalDateTime startOfDay = challengeDate.atStartOfDay();
        LocalDateTime endOfDay = challengeDate.atTime(LocalTime.MAX);

        ChallengeType type = challenge.getChallengeType();

        if (type == ChallengeType.DEPOSIT_ANY) {
            long count = goalContributionRepository.countByUserIdAndDateRange(user.getId(), startOfDay, endOfDay);
            BigDecimal progress = BigDecimal.valueOf(count);
            return ChallengeEvaluationResult.builder()
                    .progress(progress)
                    .completed(count >= 1)
                    .build();
        }

        if (type == ChallengeType.GOAL_DEPOSIT) {
            ChallengeRule rule = challenge.getRule();
            if (rule != null && rule.getRuleType() == ChallengeRuleType.DEPOSIT_COUNT) {
                int targetCount = rule.getTargetCount() != null ? rule.getTargetCount() : 1;
                long count = goalContributionRepository.countByUserIdAndDateRange(user.getId(), startOfDay, endOfDay);
                return ChallengeEvaluationResult.builder()
                        .progress(BigDecimal.valueOf(count))
                        .completed(count >= targetCount)
                        .build();
            }
        }

        // DEPOSIT_AMOUNT or default amount-based deposit
        BigDecimal target = challenge.getTargetValue();
        if (target == null && challenge.getRule() != null) {
            target = challenge.getRule().getMinimumAmount();
        }
        if (target == null) {
            target = BigDecimal.ZERO;
        }

        BigDecimal sum = goalContributionRepository.sumAmountByUserIdAndDateRange(user.getId(), startOfDay, endOfDay);
        BigDecimal progress = (sum != null) ? sum : BigDecimal.ZERO;

        boolean completed = progress.compareTo(target) >= 0;

        return ChallengeEvaluationResult.builder()
                .progress(progress)
                .completed(completed)
                .build();
    }
}
