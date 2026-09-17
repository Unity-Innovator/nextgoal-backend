package com.example.genz.reward.service;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;

import java.math.BigDecimal;

public interface GoalMilestoneRewardService {

    /**
     * Detect crossed milestones between previousAmount and newAmount, award coins according to
     * goal target tier rules (for both individual and shared goals), update user balances,
     * persist reward records, and return the coins earned by the depositing user from this deposit.
     *
     * @param goal the savings goal
     * @param depositingUser the user making the deposit
     * @param previousAmount the goal balance before deposit
     * @param newAmount the goal balance after deposit
     * @return the number of coins earned by depositingUser from this deposit
     */
    int processMilestoneRewards(Goal goal, User depositingUser, BigDecimal previousAmount, BigDecimal newAmount);

    /**
     * Check if a goal is a shared goal.
     */
    boolean isSharedGoal(Goal goal);
}
