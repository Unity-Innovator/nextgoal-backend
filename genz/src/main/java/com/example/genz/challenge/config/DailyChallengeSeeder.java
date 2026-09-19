package com.example.genz.challenge.config;

import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                log.info("Seeding default daily challenges into database...");

                List<DailyChallenge> challenges = List.of(
                    // EXPENSE_LOG challenges
                    createChallenge(
                            "Log Today's Coffee or Drink",
                            "Track the exact cost of your coffee, tea, boba, or beverage today to see where your loose change goes.",
                            ChallengeType.EXPENSE_LOG,
                            25
                    ),
                    createChallenge(
                            "Track All Meals Today",
                            "Log what you spend for breakfast, lunch, and dinner today to stay mindful of daily food expenses.",
                            ChallengeType.EXPENSE_LOG,
                            30
                    ),
                    createChallenge(
                            "Log Your Daily Transportation",
                            "Record your bus fare, train ticket, rideshare, or petrol expense for today.",
                            ChallengeType.EXPENSE_LOG,
                            25
                    ),
                    createChallenge(
                            "Zero-Spend Day Challenge",
                            "Spend zero dollars on non-essential items today and celebrate your impulse-control milestone!",
                            ChallengeType.EXPENSE_LOG,
                            50
                    ),
                    createChallenge(
                            "Audit Active Subscriptions",
                            "Review your streaming, music, app, and gym subscriptions and record the ones you actually use.",
                            ChallengeType.EXPENSE_LOG,
                            35
                    ),
                    createChallenge(
                            "Track an Impulse Purchase Urge",
                            "Write down an item you were tempted to buy on impulse today but chose to skip.",
                            ChallengeType.EXPENSE_LOG,
                            30
                    ),
                    createChallenge(
                            "Log Grocery Shopping Total",
                            "Record your grocery receipt total and check if you stayed within your planned list.",
                            ChallengeType.EXPENSE_LOG,
                            25
                    ),

                    // QUIZ challenges
                    createChallenge(
                            "The 50/30/20 Rule Check",
                            "Test your knowledge on how much of your monthly income should go toward needs, wants, and savings.",
                            ChallengeType.QUIZ,
                            40
                    ),
                    createChallenge(
                            "Emergency Fund Essentials",
                            "How many months of essential living expenses should your emergency fund cover?",
                            ChallengeType.QUIZ,
                            40
                    ),
                    createChallenge(
                            "Compound Interest Mastery",
                            "Test how compound interest turns small, consistent savings into serious long-term growth.",
                            ChallengeType.QUIZ,
                            45
                    ),
                    createChallenge(
                            "Credit Score & Utilization",
                            "Understand how your credit card utilization ratio directly impacts your credit health.",
                            ChallengeType.QUIZ,
                            40
                    ),
                    createChallenge(
                            "Inflation vs Cash Savings",
                            "Learn why uninvested cash loses purchasing power over time due to inflation.",
                            ChallengeType.QUIZ,
                            45
                    ),
                    createChallenge(
                            "Needs vs Wants Breakdown",
                            "Test your ability to distinguish between essential survival needs and lifestyle upgrade wants.",
                            ChallengeType.QUIZ,
                            35
                    ),
                    createChallenge(
                            "High-Yield Savings Accounts (HYSA)",
                            "Discover how high-yield savings accounts help your money outpace standard bank interest rates.",
                            ChallengeType.QUIZ,
                            40
                    ),

                    // TIP challenges
                    createChallenge(
                            "The 24-Hour Cooling Rule",
                            "Wait 24 hours before purchasing non-essential items over $30 to eliminate emotional spending.",
                            ChallengeType.TIP,
                            20
                    ),
                    createChallenge(
                            "Pay Yourself First",
                            "Automatically move a portion of any incoming allowance or paycheck into your goal before spending.",
                            ChallengeType.TIP,
                            30
                    ),
                    createChallenge(
                            "Automate Micro-Savings",
                            "Set up a recurring daily transfer of even $1 or $2 to build your savings effortlessly.",
                            ChallengeType.TIP,
                            25
                    ),
                    createChallenge(
                            "Unsubscribe from Retail Promos",
                            "Unsubscribe from 3 marketing email newsletters or brand SMS channels to curb flash-sale FOMO.",
                            ChallengeType.TIP,
                            25
                    ),
                    createChallenge(
                            "The 72-Hour Rule for Big Purchases",
                            "For purchases over $100, pause for 72 hours to evaluate whether the purchase aligns with your goals.",
                            ChallengeType.TIP,
                            30
                    ),
                    createChallenge(
                            "Round-Up Savings Habit",
                            "Calculate your spare change by rounding daily purchases to the next dollar and stash it away.",
                            ChallengeType.TIP,
                            25
                    )
                );

                dailyChallengeRepository.saveAll(challenges);
                log.info("Successfully seeded {} default daily challenges into the database.", challenges.size());
            }
        };
    }

    private DailyChallenge createChallenge(String title, String description, ChallengeType type, int xpReward) {
        return DailyChallenge.builder()
                .title(title)
                .description(description)
                .challengeType(type)
                .xpReward(xpReward)
                .isActive(true)
                .build();
    }
}
