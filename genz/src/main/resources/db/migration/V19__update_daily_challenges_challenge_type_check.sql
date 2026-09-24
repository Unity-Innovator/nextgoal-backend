-- Migration V19: Update daily_challenges_challenge_type_check to include all supported ChallengeType enum values

ALTER TABLE IF EXISTS daily_challenges
    DROP CONSTRAINT IF EXISTS daily_challenges_challenge_type_check;

ALTER TABLE IF EXISTS daily_challenges
    ADD CONSTRAINT daily_challenges_challenge_type_check
    CHECK (challenge_type IN (
        'DEPOSIT_AMOUNT',
        'DEPOSIT_ANY',
        'CREATE_GOAL',
        'COMPLETE_QUIZ',
        'EARN_XP',
        'INVITE_FRIEND',
        'FRIEND_JOINED',
        'REACH_GOAL_MILESTONE',
        'LOG_EXPENSE',
        'GOAL_DEPOSIT',
        'QUIZ',
        'EXPENSE_LOG',
        'TIP',
        'MANUAL'
    ));
