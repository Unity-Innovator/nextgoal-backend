package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.learning.entity.QuizAttempt;

import com.example.genz.goal.entity.Goal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ChallengeCompletionService {

    List<UserDailyChallenge> evaluateDepositChallenges(User user, BigDecimal depositAmount);

    List<UserDailyChallenge> evaluateQuizChallenges(User user, QuizAttempt quizAttempt);

    List<UserDailyChallenge> evaluateGoalChallenges(User user);

    List<UserDailyChallenge> evaluateInvitationChallenges(User user);

    List<UserDailyChallenge> evaluateFriendJoinedChallenges(User inviter);

    List<UserDailyChallenge> evaluateXpChallenges(User user, int xpEarned);

    List<UserDailyChallenge> evaluateGoalMilestoneChallenges(User user, Goal goal, BigDecimal prevAmount, BigDecimal newAmount);

    UserDailyChallenge completeManualChallenge(UUID userDailyChallengeId, User user);

    int calculateStreak(User user);
}
