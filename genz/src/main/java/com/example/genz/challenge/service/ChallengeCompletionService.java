package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.learning.entity.QuizAttempt;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ChallengeCompletionService {

    List<UserDailyChallenge> evaluateDepositChallenges(User user, BigDecimal depositAmount);

    List<UserDailyChallenge> evaluateQuizChallenges(User user, QuizAttempt quizAttempt);

    UserDailyChallenge completeManualChallenge(UUID userDailyChallengeId, User user);

    int calculateStreak(User user);
}
