package com.example.genz.challenge.config;

import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyChallengeSeeder {

    private final DailyChallengeRepository dailyChallengeRepository;

    @Bean
    public CommandLineRunner seedDailyChallenges() {
        return args -> {
            if (dailyChallengeRepository.count() == 0) {
                log.info("Seeding default trackable action-based daily challenges into database...");

                List<DailyChallenge> challenges = List.of(
                        // 1. Deposit Amount
                        createChallenge(
                                "Save at least $2 today",
                                "Deposit at least $2 into any of your active savings goals today.",
                                ChallengeType.DEPOSIT_AMOUNT,
                                new BigDecimal("2.00"),
                                25,
                                25,
                                ChallengeRule.builder()
                                        .ruleType(ChallengeRuleType.MINIMUM_DEPOSIT)
                                        .minimumAmount(new BigDecimal("2.00"))
                                        .build()
                        ),

                        // 2. Any Deposit
                        createChallenge(
                                "Make a deposit today",
                                "Add any deposit amount to one of your active savings goals today.",
                                ChallengeType.DEPOSIT_ANY,
                                new BigDecimal("1.00"),
                                15,
                                15,
                                ChallengeRule.builder()
                                        .ruleType(ChallengeRuleType.DEPOSIT_COUNT)
                                        .targetCount(1)
                                        .build()
                        ),

                        // 3. Create Goal
                        createChallenge(
                                "Create a saving goal",
                                "Start a new personal or shared saving goal to plan your future.",
                                ChallengeType.CREATE_GOAL,
                                new BigDecimal("1.00"),
                                20,
                                20,
                                ChallengeRule.builder()
                                        .targetCount(1)
                                        .build()
                        ),

                        // 4. Complete Quiz
                        createChallenge(
                                "Complete a quiz",
                                "Finish any financial literacy learning quiz to test and grow your knowledge.",
                                ChallengeType.COMPLETE_QUIZ,
                                new BigDecimal("1.00"),
                                20,
                                20,
                                ChallengeRule.builder()
                                        .ruleType(ChallengeRuleType.COMPLETE_QUIZ)
                                        .targetCount(1)
                                        .build()
                        ),

                        // 5. Earn XP
                        createChallenge(
                                "Earn 50 XP today",
                                "Earn at least 50 XP by taking quizzes and learning financial habits today.",
                                ChallengeType.EARN_XP,
                                new BigDecimal("50.00"),
                                25,
                                25,
                                null
                        ),

                        // 6. Invite Friends
                        createChallenge(
                                "Invite 2 friends",
                                "Invite 2 friends to join and save with you on NextGoal.",
                                ChallengeType.INVITE_FRIEND,
                                new BigDecimal("2.00"),
                                30,
                                30,
                                ChallengeRule.builder()
                                        .targetCount(2)
                                        .build()
                        ),

                        // 7. Reach Goal Milestone
                        createChallenge(
                                "Reach a goal milestone",
                                "Hit 25% or more on any of your savings goals today.",
                                ChallengeType.REACH_GOAL_MILESTONE,
                                new BigDecimal("25.00"),
                                30,
                                30,
                                null
                        )
                );

                dailyChallengeRepository.saveAll(challenges);
                log.info("Successfully seeded {} default trackable daily challenges into the database.", challenges.size());
            }
        };
    }

    private DailyChallenge createChallenge(
            String title,
            String description,
            ChallengeType type,
            BigDecimal targetValue,
            int rewardCoins,
            int xpReward,
            ChallengeRule rule) {
        return DailyChallenge.builder()
                .title(title)
                .description(description)
                .challengeType(type)
                .targetValue(targetValue)
                .rewardCoins(rewardCoins)
                .xpReward(xpReward)
                .rule(rule)
                .isActive(true)
                .build();
    }
}
