package com.example.genz.challenge.evaluator;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.reward.enums.GoalMilestone;
import com.example.genz.reward.repository.GoalMilestoneRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class GoalMilestoneChallengeEvaluator implements ChallengeEvaluator {

    private final GoalMilestoneRewardRepository goalMilestoneRewardRepository;

    @Override
    public boolean supports(ChallengeType challengeType) {
        return challengeType == ChallengeType.REACH_GOAL_MILESTONE;
    }

    @Override
    public ChallengeEvaluationResult evaluate(User user, DailyChallenge challenge, LocalDate challengeDate, Object context) {
        LocalDateTime startOfDay = challengeDate.atStartOfDay();
        LocalDateTime endOfDay = challengeDate.atTime(LocalTime.MAX);

        int targetMilestone = 25;
        if (challenge.getTargetValue() != null) {
            targetMilestone = challenge.getTargetValue().intValue();
        }

        GoalMilestone matchingMilestone = switch (targetMilestone) {
            case 50 -> GoalMilestone.MILESTONE_50;
            case 75 -> GoalMilestone.MILESTONE_75;
            case 100 -> GoalMilestone.MILESTONE_100;
            default -> GoalMilestone.MILESTONE_25;
        };

        long specificCount = goalMilestoneRewardRepository.countByUserIdAndMilestoneAndCreatedAtBetween(
                user.getId(),
                matchingMilestone,
                startOfDay,
                endOfDay
        );

        long anyMilestoneCount = goalMilestoneRewardRepository.countByUserIdAndCreatedAtBetween(
                user.getId(),
                startOfDay,
                endOfDay
        );

        boolean completed = specificCount > 0 || (targetMilestone <= 25 && anyMilestoneCount > 0);
        BigDecimal progress = completed ? BigDecimal.valueOf(targetMilestone) : BigDecimal.ZERO;

        return ChallengeEvaluationResult.builder()
                .progress(progress)
                .completed(completed)
                .build();
    }
}
