package com.example.genz.security.config;

import com.example.genz.entity.Reward.Badge;
import com.example.genz.enums.BadgeCategory;
import com.example.genz.enums.BadgeRarity;
import com.example.genz.enums.BadgeRequirementType;
import com.example.genz.repository.RewardRepository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AchievementSeeder {

    private final BadgeRepository badgeRepository;

    @Bean
    CommandLineRunner seedAchievements() {
        return args -> {
            if (badgeRepository.count() == 0) {
                List<Badge> defaultBadges = List.of(
                    // 1. GETTING STARTED (6)
                    createBadge("First Goal Created", "Created your first savings goal", "badge_first_goal.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.FIRST_GOAL_CREATED, 1, 50, BadgeRarity.COMMON),
                    createBadge("First Deposit", "Made your first deposit into a goal", "badge_first_deposit.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.FIRST_DEPOSIT, 1, 100, BadgeRarity.COMMON),
                    createBadge("Profile Completed", "Completed your profile details", "badge_profile.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.PROFILE_COMPLETED, 1, 50, BadgeRarity.COMMON),
                    createBadge("Linked Bank Account", "Linked your first bank account", "badge_bank.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.BANK_ACCOUNT_LINKED, 1, 100, BadgeRarity.COMMON),
                    createBadge("Added Debit Card", "Added a debit card for payments", "badge_debit_card.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.DEBIT_CARD_ADDED, 1, 100, BadgeRarity.COMMON),
                    createBadge("Verified Account", "Verified your identity and account status", "badge_verified.png", BadgeCategory.GETTING_STARTED, BadgeRequirementType.ACCOUNT_VERIFIED, 1, 150, BadgeRarity.COMMON),

                    // 2. SAVING STREAKS (8)
                    createBadge("3-Day Streak", "Maintain your savings streak for 3 days", "badge_streak_3.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 3, 100, BadgeRarity.COMMON),
                    createBadge("7-Day Streak", "Maintain your savings streak for 7 days", "badge_streak_7.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 7, 150, BadgeRarity.COMMON),
                    createBadge("14-Day Streak", "Maintain your savings streak for 14 days", "badge_streak_14.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 14, 200, BadgeRarity.RARE),
                    createBadge("30-Day Streak", "Maintain your savings streak for 30 days", "badge_streak_30.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 30, 500, BadgeRarity.RARE),
                    createBadge("60-Day Streak", "Maintain your savings streak for 60 days", "badge_streak_60.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 60, 750, BadgeRarity.EPIC),
                    createBadge("90-Day Streak", "Maintain your savings streak for 90 days", "badge_streak_90.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 90, 1000, BadgeRarity.EPIC),
                    createBadge("180-Day Streak", "Maintain your savings streak for 180 days", "badge_streak_180.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 180, 1500, BadgeRarity.LEGENDARY),
                    createBadge("365-Day Streak", "Maintain your savings streak for 365 days", "badge_streak_365.png", BadgeCategory.SAVING_STREAKS, BadgeRequirementType.STREAK_DAYS, 365, 2500, BadgeRarity.LEGENDARY),

                    // 3. SAVINGS MILESTONES (7)
                    createBadge("Saved $100", "Save a total of $100", "badge_save_100.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 100, 100, BadgeRarity.COMMON),
                    createBadge("Saved $500", "Save a total of $500", "badge_save_500.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 500, 200, BadgeRarity.COMMON),
                    createBadge("Saved $1,000", "Save a total of $1,000", "badge_save_1000.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 1000, 500, BadgeRarity.RARE),
                    createBadge("Saved $5,000", "Save a total of $5,000", "badge_save_5000.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 5000, 750, BadgeRarity.RARE),
                    createBadge("Saved $10,000", "Save a total of $10,000", "badge_save_10000.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 10000, 1000, BadgeRarity.EPIC),
                    createBadge("Saved $50,000", "Save a total of $50,000", "badge_save_50000.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 50000, 2000, BadgeRarity.EPIC),
                    createBadge("Big Saver ($100,000)", "Save an impressive total of $100,000", "badge_save_100000.png", BadgeCategory.SAVINGS_MILESTONES, BadgeRequirementType.TOTAL_SAVED, 100000, 3000, BadgeRarity.LEGENDARY),

                    // 4. GOALS ACHIEVED (6)
                    createBadge("First Goal Completed", "Complete your first savings goal", "badge_goal_1.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.GOALS_COMPLETED, 1, 150, BadgeRarity.COMMON),
                    createBadge("3 Goals Completed", "Successfully complete 3 savings goals", "badge_goal_3.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.GOALS_COMPLETED, 3, 300, BadgeRarity.RARE),
                    createBadge("5 Goals Completed", "Successfully complete 5 savings goals", "badge_goal_5.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.GOALS_COMPLETED, 5, 500, BadgeRarity.RARE),
                    createBadge("10 Goals Completed", "Successfully complete 10 savings goals", "badge_goal_10.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.GOALS_COMPLETED, 10, 1000, BadgeRarity.EPIC),
                    createBadge("Overachiever", "Successfully complete 25 savings goals", "badge_overachiever.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.GOALS_COMPLETED, 25, 2000, BadgeRarity.LEGENDARY),
                    createBadge("Multi-Goal Master", "Create and manage at least 3 active savings goals", "badge_multigoal.png", BadgeCategory.GOALS_ACHIEVED, BadgeRequirementType.MULTI_GOAL_MASTER, 3, 250, BadgeRarity.RARE),

                    // 5. COMMUNITY & SOCIAL (5)
                    createBadge("Invited a Friend", "Invite a friend to join the platform", "badge_invite.png", BadgeCategory.COMMUNITY_SOCIAL, BadgeRequirementType.FRIEND_INVITED, 1, 100, BadgeRarity.COMMON),
                    createBadge("First Post", "Share your first post with the community", "badge_post.png", BadgeCategory.COMMUNITY_SOCIAL, BadgeRequirementType.FIRST_POST, 1, 50, BadgeRarity.COMMON),
                    createBadge("Helpful Hand", "Perform 5 helpful community actions", "badge_helpful.png", BadgeCategory.COMMUNITY_SOCIAL, BadgeRequirementType.HELPFUL_ACTIONS, 5, 150, BadgeRarity.RARE),
                    createBadge("Community Star", "Reach 10 community contributions", "badge_star.png", BadgeCategory.COMMUNITY_SOCIAL, BadgeRequirementType.COMMUNITY_CONTRIBUTIONS, 10, 300, BadgeRarity.RARE),
                    createBadge("Top Contributor", "Reach 25 community contributions", "badge_top_contributor.png", BadgeCategory.COMMUNITY_SOCIAL, BadgeRequirementType.COMMUNITY_CONTRIBUTIONS, 25, 750, BadgeRarity.EPIC),

                    // 6. WALLET & TRANSACTIONS (6)
                    createBadge("First Transfer", "Complete your first money transfer", "badge_transfer.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.FIRST_TRANSFER, 1, 50, BadgeRarity.COMMON),
                    createBadge("First Withdrawal", "Perform your first withdrawal", "badge_withdrawal.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.FIRST_WITHDRAWAL, 1, 50, BadgeRarity.COMMON),
                    createBadge("QR Pro", "Complete 5 payments via QR code", "badge_qr.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.QR_PAYMENT, 5, 150, BadgeRarity.RARE),
                    createBadge("Auto-Save Activated", "Activate automatic savings feature", "badge_autosave.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.AUTO_SAVE_ACTIVATED, 1, 150, BadgeRarity.RARE),
                    createBadge("Reward Redeemed", "Redeem your first reward perk", "badge_redeemed.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.REWARD_REDEEMED, 1, 100, BadgeRarity.COMMON),
                    createBadge("Smart Spender", "Complete 10 smart wallet transactions", "badge_smart_spender.png", BadgeCategory.WALLET_TRANSACTIONS, BadgeRequirementType.SMART_SPENDER, 10, 300, BadgeRarity.RARE),

                    // 7. ENGAGEMENT & APP USAGE (5)
                    createBadge("Early Bird", "Log in and save early in the morning", "badge_early_bird.png", BadgeCategory.ENGAGEMENT_APP_USAGE, BadgeRequirementType.EARLY_ACTIVITY, 1, 50, BadgeRarity.COMMON),
                    createBadge("Night Owl", "Log in and check goals late at night", "badge_night_owl.png", BadgeCategory.ENGAGEMENT_APP_USAGE, BadgeRequirementType.NIGHT_ACTIVITY, 1, 50, BadgeRarity.COMMON),
                    createBadge("App Explorer", "Explore key screens across the application", "badge_explorer.png", BadgeCategory.ENGAGEMENT_APP_USAGE, BadgeRequirementType.APP_EXPLORER, 1, 50, BadgeRarity.COMMON),
                    createBadge("Quiz Completed", "Complete your first financial literacy quiz", "badge_quiz.png", BadgeCategory.ENGAGEMENT_APP_USAGE, BadgeRequirementType.QUIZ_COMPLETED, 1, 100, BadgeRarity.COMMON),
                    createBadge("Feedback Given", "Submit feedback to help us improve", "badge_feedback.png", BadgeCategory.ENGAGEMENT_APP_USAGE, BadgeRequirementType.FEEDBACK_GIVEN, 1, 100, BadgeRarity.COMMON),

                    // 8. SPECIAL & SEASONAL (5)
                    createBadge("New Year New Goal", "Create a goal during New Year season", "badge_new_year.png", BadgeCategory.SPECIAL_SEASONAL, BadgeRequirementType.SPECIAL_NEW_YEAR, 1, 200, BadgeRarity.RARE),
                    createBadge("Holiday Hero", "Participate in a holiday savings event", "badge_holiday.png", BadgeCategory.SPECIAL_SEASONAL, BadgeRequirementType.SPECIAL_HOLIDAY, 1, 200, BadgeRarity.RARE),
                    createBadge("Challenge Champion", "Complete 5 daily challenges", "badge_challenge.png", BadgeCategory.SPECIAL_SEASONAL, BadgeRequirementType.CHALLENGE_COMPLETED, 5, 300, BadgeRarity.RARE),
                    createBadge("Perfect Month", "Maintain active savings every day of a month", "badge_perfect_month.png", BadgeCategory.SPECIAL_SEASONAL, BadgeRequirementType.PERFECT_MONTH, 1, 500, BadgeRarity.EPIC),
                    createBadge("Legend Status", "Achieve ultimate gamification status", "badge_legend.png", BadgeCategory.SPECIAL_SEASONAL, BadgeRequirementType.LEGEND_STATUS, 1, 2000, BadgeRarity.LEGENDARY)
                );

                badgeRepository.saveAll(defaultBadges);
                System.out.println("Seeded 48 achievement badges into database.");
            }
        };
    }

    private Badge createBadge(String name, String description, String iconUrl, BadgeCategory category, BadgeRequirementType type, Integer reqValue, Integer xpReward, BadgeRarity rarity) {
        return Badge.builder()
                .name(name)
                .description(description)
                .iconUrl(iconUrl)
                .category(category)
                .requirementType(type)
                .requirementValue(reqValue)
                .xpReward(xpReward)
                .rarity(rarity)
                .active(true)
                .build();
    }
}
