package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.evaluator.ChallengeEvaluationResult;
import com.example.genz.challenge.evaluator.ChallengeEvaluator;
import com.example.genz.challenge.evaluator.ChallengeEvaluatorRegistry;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.entity.Goal;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeCompletionServiceImpl implements ChallengeCompletionService {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final UserDailyChallengeRepository userDailyChallengeRepository;
    private final UserRepository userRepository;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementService achievementService;
    private final ChallengeEvaluatorRegistry evaluatorRegistry;
    private final com.example.genz.goal.repository.GoalContributionRepository goalContributionRepository;
    private final com.example.genz.transaction.repository.WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateDepositChallenges(User user, BigDecimal depositAmount) {
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.DEPOSIT_AMOUNT, ChallengeType.DEPOSIT_ANY, ChallengeType.GOAL_DEPOSIT),
                depositAmount
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateQuizChallenges(User user, QuizAttempt quizAttempt) {
        if (user == null || quizAttempt == null || !Boolean.TRUE.equals(quizAttempt.getIsCompleted())) {
            return Collections.emptyList();
        }
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.COMPLETE_QUIZ, ChallengeType.QUIZ),
                quizAttempt
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateGoalChallenges(User user) {
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.CREATE_GOAL),
                null
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateInvitationChallenges(User user) {
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.INVITE_FRIEND),
                null
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateFriendJoinedChallenges(User inviter) {
        return evaluateChallengesForTypes(
                inviter,
                Set.of(ChallengeType.FRIEND_JOINED),
                null
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateXpChallenges(User user, int xpEarned) {
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.EARN_XP),
                xpEarned
        );
    }

    @Override
    @Transactional
    public List<UserDailyChallenge> evaluateGoalMilestoneChallenges(User user, Goal goal, BigDecimal prevAmount, BigDecimal newAmount) {
        return evaluateChallengesForTypes(
                user,
                Set.of(ChallengeType.REACH_GOAL_MILESTONE),
                Map.of("goal", goal, "prev", prevAmount, "new", newAmount)
        );
    }

    private List<UserDailyChallenge> evaluateChallengesForTypes(User user, Set<ChallengeType> types, Object context) {
        if (user == null || types == null || types.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<DailyChallenge> activeChallenges = dailyChallengeRepository.findByChallengeTypeInAndIsActiveTrue(types);
        if (activeChallenges.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserDailyChallenge> processedList = new ArrayList<>();

        for (DailyChallenge challenge : activeChallenges) {
            Optional<ChallengeEvaluator> evaluatorOpt = evaluatorRegistry.getEvaluator(challenge.getChallengeType());
            if (evaluatorOpt.isEmpty()) {
                continue;
            }

            UserDailyChallenge udc = userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(user, challenge, today)
                    .orElseGet(() -> UserDailyChallenge.builder()
                            .user(user)
                            .challenge(challenge)
                            .challengeDate(today)
                            .status(ChallengeStatus.PENDING)
                            .progress(BigDecimal.ZERO)
                            .build());

            if (udc.getStatus() == ChallengeStatus.COMPLETED) {
                ChallengeEvaluationResult result = evaluatorOpt.get().evaluate(user, challenge, today, context);
                if (result.getProgress() != null && result.getProgress().compareTo(udc.getProgress()) > 0) {
                    udc.setProgress(result.getProgress());
                    userDailyChallengeRepository.save(udc);
                }
                continue;
            }

            ChallengeEvaluationResult result = evaluatorOpt.get().evaluate(user, challenge, today, context);
            BigDecimal newProgress = result.getProgress() != null ? result.getProgress() : BigDecimal.ZERO;
            udc.setProgress(newProgress);

            if (result.isCompleted()) {
                markCompletedAndAwardRewards(user, udc, challenge);
                processedList.add(udc);
            } else {
                udc.setStatus(newProgress.compareTo(BigDecimal.ZERO) > 0 ? ChallengeStatus.IN_PROGRESS : ChallengeStatus.PENDING);
                userDailyChallengeRepository.save(udc);
            }
        }

        return processedList;
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

        markCompletedAndAwardRewards(user, udc, challenge);
        return udc;
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateStreak(User user) {
        return calculateStreak(user, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateStreak(User user, LocalDate asOfDate) {
        if (user == null || user.getId() == null) {
            return 0;
        }

        LocalDate referenceDate = asOfDate != null ? asOfDate : LocalDate.now();
        Set<LocalDate> dateSet = getQualifyingActivityDates(user.getId());
        if (dateSet.isEmpty()) {
            return 0;
        }

        LocalDate checkDate;
        if (dateSet.contains(referenceDate)) {
            checkDate = referenceDate;
        } else if (dateSet.contains(referenceDate.minusDays(1))) {
            checkDate = referenceDate.minusDays(1);
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

    @Override
    @Transactional
    public int updateStreak(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }

        int newStreak = calculateStreak(user);
        UserReward userReward = achievementService.getOrCreateUserReward(user);
        userReward.setCurrentStreak(newStreak);
        if (userReward.getLongestStreak() == null || newStreak > userReward.getLongestStreak()) {
            userReward.setLongestStreak(newStreak);
        }
        userReward.setLastActivityDate(LocalDate.now());
        userRewardRepository.save(userReward);

        try {
            achievementService.evaluateAndUnlockAchievements(user, BadgeRequirementType.STREAK_DAYS, Map.of("streak", newStreak));
        } catch (Exception e) {
            log.warn("Failed to evaluate streak badges for user {}: {}", user.getId(), e.getMessage());
        }

        return newStreak;
    }

    private Set<LocalDate> getQualifyingActivityDates(UUID userId) {
        Set<LocalDate> dates = new HashSet<>();

        // 1. Completed daily challenges
        List<LocalDate> completedDates = userDailyChallengeRepository.findDistinctCompletedDatesByUserId(userId);
        if (completedDates != null) {
            dates.addAll(completedDates);
        }

        // 2. Goal contributions
        if (goalContributionRepository != null) {
            List<LocalDateTime> contributionTimes = goalContributionRepository.findDistinctContributedAtByUserId(userId);
            if (contributionTimes != null) {
                for (LocalDateTime dt : contributionTimes) {
                    if (dt != null) {
                        dates.add(dt.toLocalDate());
                    }
                }
            }
        }

        // 3. Wallet deposit transactions
        if (walletTransactionRepository != null) {
            List<LocalDateTime> depositTimes = walletTransactionRepository.findDistinctDepositCreatedAtByUserId(userId);
            if (depositTimes != null) {
                for (LocalDateTime dt : depositTimes) {
                    if (dt != null) {
                        dates.add(dt.toLocalDate());
                    }
                }
            }
        }

        return dates;
    }

    private void markCompletedAndAwardRewards(User user, UserDailyChallenge udc, DailyChallenge challenge) {
        if (udc.getStatus() == ChallengeStatus.COMPLETED) {
            return;
        }

        int rewardCoins = (challenge != null && challenge.getRewardCoins() != null) ? challenge.getRewardCoins() : 0;
        int xpReward = (challenge != null && challenge.getXpReward() != null) ? challenge.getXpReward() : 0;

        udc.setStatus(ChallengeStatus.COMPLETED);
        udc.setCompletedAt(LocalDateTime.now());
        udc.setRewardCoinsAwarded(rewardCoins);
        udc.setXpAwarded(xpReward);
        userDailyChallengeRepository.save(udc);

        // 1. Award coins once
        if (rewardCoins > 0) {
            int currentCoins = user.getCoinBalance() != null ? user.getCoinBalance() : 0;
            user.setCoinBalance(currentCoins + rewardCoins);
            userRepository.save(user);
            log.info("Awarded {} coins to user {} for completing challenge {}", rewardCoins, user.getId(), challenge != null ? challenge.getTitle() : "");
        }

        // 2. Award XP once
        if (xpReward > 0 && !xpTransactionRepository.existsByUserAndActivityTypeAndReferenceId(user, ActivityType.DAILY_CHALLENGE, udc.getId())) {
            int currentXp = user.getTotalXp() != null ? user.getTotalXp() : 0;
            user.setTotalXp(currentXp + xpReward);
            userRepository.save(user);

            UserReward userReward = achievementService.getOrCreateUserReward(user);
            userReward.addXp(xpReward);
            userRewardRepository.save(userReward);

            XpTransaction xpTransaction = XpTransaction.builder()
                    .user(user)
                    .activityType(ActivityType.DAILY_CHALLENGE)
                    .referenceId(udc.getId())
                    .points(xpReward)
                    .build();
            xpTransactionRepository.save(xpTransaction);
        }

        // 3. Update streak on qualifying challenge completion
        updateStreak(user);
    }
}
