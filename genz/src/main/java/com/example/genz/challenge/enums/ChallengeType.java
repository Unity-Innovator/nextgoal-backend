package com.example.genz.challenge.enums;

public enum ChallengeType {
    // Action-based trackable challenge types
    DEPOSIT_AMOUNT,
    DEPOSIT_ANY,
    CREATE_GOAL,
    COMPLETE_QUIZ,
    EARN_XP,
    INVITE_FRIEND,
    FRIEND_JOINED,
    REACH_GOAL_MILESTONE,
    LOG_EXPENSE,

    // Legacy types retained for backwards compatibility
    GOAL_DEPOSIT,
    QUIZ,
    EXPENSE_LOG,
    TIP,
    MANUAL
}
