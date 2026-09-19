package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.reward.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeCompletionServiceImpl implements ChallengeCompletionService {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final UserDailyChallengeRepository userDailyChallengeRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final UserRepository userRepository;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementService achievementService;

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateDepositChallenges(User user, BigDecimal depositAmount) {
        if (user == null) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<DailyChallenge> depositChallenges = dailyChallengeRepository.findByChallengeTypeAndIsActiveTrue(ChallengeType.GOAL_DEPOSIT);
        if (depositChallenges.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal todayTotalSaved = goalContributionRepository.sumAmountByUserIdAndDateRange(user.getId(), startOfDay, endOfDay);
        long todayDepositCount = goalContributionRepository.countByUserIdAndDateRange(user.getId(), startOfDay, endOfDay);

        List<UserDailyChallenge> completedList = new ArrayList<>();

        for (DailyChallenge challenge : depositChallenges) {
            boolean alreadyCompleted = userDailyChallengeRepository.existsByUserAndChallengeAndChallengeDateAndStatus(
                    user, challenge, today, ChallengeStatus.COMPLETED
            );
            if (alreadyCompleted) {
                continue;
            }

            ChallengeRule rule = challenge.getRule();
            boolean satisfied;

            if (rule != null && rule.getRuleType() == ChallengeRuleType.DEPOSIT_COUNT) {
                int requiredCount = rule.getTargetCount() != null ? rule.getTargetCount() : 1;
                satisfied = todayDepositCount >= requiredCount;
            } else {
                BigDecimal minAmount = (rule != null && rule.getMinimumAmount() != null)
                        ? rule.getMinimumAmount()
                        : BigDecimal.ZERO;
                satisfied = todayTotalSaved != null && todayTotalSaved.compareTo(minAmount) >= 0;
            }

            if (satisfied) {
                UserDailyChallenge udc = userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(user, challenge, today)
                        .orElseGet(() -> UserDailyChallenge.builder()
                                .user(user)
                                .challenge(challenge)
                                .challengeDate(today)
                                .status(ChallengeStatus.PENDING)
                                .build());
                markCompletedAndAwardXp(user, udc, challenge);
                completedList.add(udc);
            }
        }

        return completedList;
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateQuizChallenges(User user, QuizAttempt quizAttempt) {
        if (user == null || quizAttempt == null || !Boolean.TRUE.equals(quizAttempt.getIsCompleted())) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<DailyChallenge> quizChallenges = dailyChallengeRepository.findByChallengeTypeAndIsActiveTrue(ChallengeType.QUIZ);
        if (quizChallenges.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserDailyChallenge> completedList = new ArrayList<>();
        for (DailyChallenge challenge : quizChallenges) {
            boolean alreadyCompleted = userDailyChallengeRepository.existsByUserAndChallengeAndChallengeDateAndStatus(
                    user, challenge, today, ChallengeStatus.COMPLETED
            );
            if (alreadyCompleted) {
                continue;
            }

            UserDailyChallenge udc = userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(user, challenge, today)
                    .orElseGet(() -> UserDailyChallenge.builder()
                            .user(user)
                            .challenge(challenge)
                            .challengeDate(today)
                            .status(ChallengeStatus.PENDING)
                            .build());
            markCompletedAndAwardXp(user, udc, challenge);
            completedList.add(udc);
        }

        return completedList;
    }

    @Override
    @Transactional
    public UserDailyChallenge completeManualChallenge(UUID userDailyChallengeId, User user) {
        UserDailyChallenge udc = userDailyChallengeRepository.findById(userDailyChallengeId)
                .orElseThrow(() -> new ResourceNoteFoundException("Daily challenge assignment not found with id: " + userDailyChallengeId));

        if (!udc.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to complete another user's daily challenge.");
        }

        DailyChallenge challenge = udc.getChallenge();
        if (challenge.isAutomaticallyVerifiable()) {
            throw new BadRequestException("This challenge (" + challenge.getTitle() + ") is verified automatically based on your actual activity and cannot be completed manually.");
        }

        if (udc.getStatus() == ChallengeStatus.COMPLETED) {
            return udc;
        }

        markCompletedAndAwardXp(user, udc, challenge);
        return udc;
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateStreak(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }

        List<LocalDate> completedDates = userDailyChallengeRepository.findDistinctCompletedDatesByUserId(user.getId());
        if (completedDates == null || completedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        Set<LocalDate> dateSet = new HashSet<>(completedDates);

        LocalDate checkDate;
        if (dateSet.contains(today)) {
            checkDate = today;
        } else if (dateSet.contains(today.minusDays(1))) {
            checkDate = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    private void markCompletedAndAwardXp(User user, UserDailyChallenge udc, DailyChallenge challenge) {
        if (udc.getStatus() == ChallengeStatus.COMPLETED) {
            return;
        }

        int xpReward = (challenge != null && challenge.getXpReward() != null) ? challenge.getXpReward() : 0;
        udc.setStatus(ChallengeStatus.COMPLETED);
        udc.setCompletedAt(LocalDateTime.now());
        udc.setXpAwarded(xpReward);
        userDailyChallengeRepository.save(udc);

        if (xpReward > 0 && !xpTransactionRepository.existsByUserAndActivityTypeAndReferenceId(user, ActivityType.DAILY_CHALLENGE, udc.getId())) {
            int currentXp = user.getTotalXp() != null ? user.getTotalXp() : 0;
            user.setTotalXp(currentXp + xpReward);
            userRepository.save(user);

            UserReward userReward = achievementService.getOrCreateUserReward(user);
            userReward.addXp(xpReward);

            int newStreak = calculateStreak(user);
            userReward.setCurrentStreak(newStreak);
            if (userReward.getLongestStreak() == null || newStreak > userReward.getLongestStreak()) {
                userReward.setLongestStreak(newStreak);
            }
            userReward.setLastActivityDate(LocalDate.now());
            userRewardRepository.save(userReward);

            XpTransaction xpTransaction = XpTransaction.builder()
                    .user(user)
                    .activityType(ActivityType.DAILY_CHALLENGE)
                    .referenceId(udc.getId())
                    .points(xpReward)
                    .build();
            xpTransactionRepository.save(xpTransaction);

            try {
                achievementService.evaluateAndUnlockAchievements(user, BadgeRequirementType.STREAK_DAYS, Map.of("streak", newStreak));
            } catch (Exception e) {
                log.warn("Failed to evaluate streak badges for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
