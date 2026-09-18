package com.example.genz.reward.service;

import com.example.genz.reward.enums.GoalMilestone;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class GoalMilestoneRewardTierService {

    private static final BigDecimal AMOUNT_50 = new BigDecimal("50");
    private static final BigDecimal AMOUNT_200 = new BigDecimal("200");
    private static final BigDecimal AMOUNT_500 = new BigDecimal("500");
    private static final BigDecimal AMOUNT_1000 = new BigDecimal("1000");
    private static final BigDecimal AMOUNT_2000 = new BigDecimal("2000");
    private static final BigDecimal AMOUNT_5000 = new BigDecimal("5000");
    private static final BigDecimal AMOUNT_10000 = new BigDecimal("10000");
    private static final BigDecimal AMOUNT_15000 = new BigDecimal("15000");
    private static final BigDecimal AMOUNT_20000 = new BigDecimal("20000");

    // Tier reward definitions: 25%, 50%, 75%, 100%
    private static final Map<GoalMilestone, Integer> TIER_50_199 = Map.of(
            GoalMilestone.MILESTONE_25, 10,
            GoalMilestone.MILESTONE_50, 20,
            GoalMilestone.MILESTONE_75, 30,
            GoalMilestone.MILESTONE_100, 75
    );

    private static final Map<GoalMilestone, Integer> TIER_200_499 = Map.of(
            GoalMilestone.MILESTONE_25, 15,
            GoalMilestone.MILESTONE_50, 30,
            GoalMilestone.MILESTONE_75, 45,
            GoalMilestone.MILESTONE_100, 100
    );

    private static final Map<GoalMilestone, Integer> TIER_500_999 = Map.of(
            GoalMilestone.MILESTONE_25, 25,
            GoalMilestone.MILESTONE_50, 50,
            GoalMilestone.MILESTONE_75, 75,
            GoalMilestone.MILESTONE_100, 150
    );

    private static final Map<GoalMilestone, Integer> TIER_1000_1999 = Map.of(
            GoalMilestone.MILESTONE_25, 40,
            GoalMilestone.MILESTONE_50, 80,
            GoalMilestone.MILESTONE_75, 120,
            GoalMilestone.MILESTONE_100, 250
    );

    private static final Map<GoalMilestone, Integer> TIER_2000_4999 = Map.of(
            GoalMilestone.MILESTONE_25, 60,
            GoalMilestone.MILESTONE_50, 120,
            GoalMilestone.MILESTONE_75, 180,
            GoalMilestone.MILESTONE_100, 350
    );

    private static final Map<GoalMilestone, Integer> TIER_5000_9999 = Map.of(
            GoalMilestone.MILESTONE_25, 80,
            GoalMilestone.MILESTONE_50, 160,
            GoalMilestone.MILESTONE_75, 240,
            GoalMilestone.MILESTONE_100, 500
    );

    private static final Map<GoalMilestone, Integer> TIER_10000_14999 = Map.of(
            GoalMilestone.MILESTONE_25, 100,
            GoalMilestone.MILESTONE_50, 200,
            GoalMilestone.MILESTONE_75, 300,
            GoalMilestone.MILESTONE_100, 700
    );

    private static final Map<GoalMilestone, Integer> TIER_15000_19999 = Map.of(
            GoalMilestone.MILESTONE_25, 125,
            GoalMilestone.MILESTONE_50, 250,
            GoalMilestone.MILESTONE_75, 375,
            GoalMilestone.MILESTONE_100, 900
    );

    private static final Map<GoalMilestone, Integer> TIER_20000_PLUS = Map.of(
            GoalMilestone.MILESTONE_25, 150,
            GoalMilestone.MILESTONE_50, 300,
            GoalMilestone.MILESTONE_75, 450,
            GoalMilestone.MILESTONE_100, 1000
    );

    /**
     * Determine coin reward for a given target amount and milestone.
     */
    public int getRewardCoins(BigDecimal targetAmount, GoalMilestone milestone) {
        if (targetAmount == null || milestone == null) {
            return 0;
        }

        Map<GoalMilestone, Integer> tierMap = getTierRewardMap(targetAmount);
        return tierMap.getOrDefault(milestone, 0);
    }

    /**
     * Map target amount to reward map based on explicit tier ranges.
     */
    public Map<GoalMilestone, Integer> getTierRewardMap(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(AMOUNT_50) < 0) {
            return Map.of();
        }

        if (targetAmount.compareTo(AMOUNT_200) < 0) {
            return TIER_50_199;
        } else if (targetAmount.compareTo(AMOUNT_500) < 0) {
            return TIER_200_499;
        } else if (targetAmount.compareTo(AMOUNT_1000) < 0) {
            return TIER_500_999;
        } else if (targetAmount.compareTo(AMOUNT_2000) < 0) {
            return TIER_1000_1999;
        } else if (targetAmount.compareTo(AMOUNT_5000) < 0) {
            return TIER_2000_4999;
        } else if (targetAmount.compareTo(AMOUNT_10000) < 0) {
            return TIER_5000_9999;
        } else if (targetAmount.compareTo(AMOUNT_15000) < 0) {
            return TIER_10000_14999;
        } else if (targetAmount.compareTo(AMOUNT_20000) < 0) {
            return TIER_15000_19999;
        } else {
            return TIER_20000_PLUS;
        }
    }
}
