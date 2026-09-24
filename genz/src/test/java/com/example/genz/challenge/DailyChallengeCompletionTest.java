package com.example.genz.challenge;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.challenge.service.DailyChallengeService;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.learning.repository.QuizAttemptRepository;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.transaction.entity.GoalContribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_challenge_completion_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class DailyChallengeCompletionTest {

    @Autowired
    private ChallengeCompletionService challengeCompletionService;

    @Autowired
    private DailyChallengeService dailyChallengeService;

    @Autowired
    private DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    private User testUser;
    private Goal testGoal;
    private GoalMember testGoalMember;
    private DailyChallenge depositChallenge;
    private DailyChallenge quizChallenge;
    private DailyChallenge manualChallenge;

    @BeforeEach
    void setUp() {
        userDailyChallengeRepository.deleteAll();
        goalContributionRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        xpTransactionRepository.deleteAll();
        goalMemberRepository.deleteAll();
        goalRepository.deleteAll();
        dailyChallengeRepository.deleteAll();

        testUser = new User();
        testUser.setFullName("Saver Test");
        testUser.setPhoneNumber("0912345678");
        testUser.setGender(Gender.MALE);
        testUser.setStatus("ACTIVE");
        testUser.setDateOfBirth(LocalDate.of(2000, 1, 1));
        testUser.setTotalXp(0);
        testUser = userRepository.save(testUser);

        testGoal = new Goal();
        testGoal.setGoalName("Emergency Fund");
        testGoal.setTargetAmount(new BigDecimal("1000.00"));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setCategory(GoalCategory.OTHER);
        testGoal.setTargetDate(LocalDate.now().plusMonths(6));
        testGoal.setOwner(testUser);
        testGoal.setCreatedAt(LocalDateTime.now());
        testGoal.setUpdatedAt(LocalDateTime.now());
        testGoal = goalRepository.save(testGoal);

        testGoalMember = new GoalMember();
        testGoalMember.setGoal(testGoal);
        testGoalMember.setUser(testUser);
        testGoalMember.setRole(MemberRole.OWNER);
        testGoalMember.setStatus(MemberStatus.ACTIVE);
        testGoalMember = goalMemberRepository.save(testGoalMember);

        depositChallenge = DailyChallenge.builder()
                .title("Save $10 Today")
                .description("Deposit at least $10 into any savings goal")
                .challengeType(ChallengeType.GOAL_DEPOSIT)
                .rule(ChallengeRule.builder()
                        .ruleType(ChallengeRuleType.MINIMUM_DEPOSIT)
                        .minimumAmount(new BigDecimal("10.00"))
                        .build())
                .xpReward(30)
                .isActive(true)
                .build();
        depositChallenge = dailyChallengeRepository.save(depositChallenge);

        quizChallenge = DailyChallenge.builder()
                .title("Complete Daily Quiz")
                .description("Finish any quiz today")
                .challengeType(ChallengeType.QUIZ)
                .rule(ChallengeRule.builder()
                        .ruleType(ChallengeRuleType.COMPLETE_QUIZ)
                        .targetCount(1)
                        .build())
                .xpReward(40)
                .isActive(true)
                .build();
        quizChallenge = dailyChallengeRepository.save(quizChallenge);

        manualChallenge = DailyChallenge.builder()
                .title("Pay Yourself First Tip")
                .description("Acknowledge reading today's smart money tip")
                .challengeType(ChallengeType.TIP)
                .rule(ChallengeRule.builder()
                        .ruleType(ChallengeRuleType.MANUAL_CONFIRM)
                        .build())
                .xpReward(20)
                .isActive(true)
                .build();
        manualChallenge = dailyChallengeRepository.save(manualChallenge);
    }

    private void recordContribution(BigDecimal amount) {
        GoalContribution contribution = new GoalContribution();
        contribution.setGoal(testGoal);
        contribution.setGoalMember(testGoalMember);
        contribution.setAmount(amount);
        contribution.setContributedAt(LocalDateTime.now());
        goalContributionRepository.save(contribution);
    }

    @Test
    @DisplayName("Scenario 1: Partial deposit ($6 of $10) does NOT complete challenge and awards 0 XP")
    void scenario1_PartialDeposit_DoesNotComplete() {
        recordContribution(new BigDecimal("6.00"));

        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("6.00"));

        List<UserDailyChallenge> list = userDailyChallengeRepository.findAllByUserAndChallengeDate(testUser, LocalDate.now());
        assertTrue(list.isEmpty() || list.stream().noneMatch(u -> u.getStatus() == ChallengeStatus.COMPLETED));

        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, refreshedUser.getTotalXp());
        assertEquals(0, xpTransactionRepository.count());
    }

    @Test
    @DisplayName("Scenario 2: Reaching threshold ($6 + $5 = $11 >= $10) completes challenge and awards 30 XP")
    void scenario2_ThresholdMet_CompletesChallengeAndAwardsXp() {
        recordContribution(new BigDecimal("6.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("6.00"));

        recordContribution(new BigDecimal("5.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("5.00"));

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, depositChallenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.COMPLETED, udc.getStatus());
        assertEquals(30, udc.getXpAwarded());
        assertNotNull(udc.getCompletedAt());

        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(30, refreshedUser.getTotalXp());

        UserReward reward = userRewardRepository.findByUser(testUser).orElseThrow();
        assertEquals(30, reward.getTotalXp());
        assertEquals(1, reward.getCurrentStreak());

        assertEquals(1, xpTransactionRepository.count());
        var tx = xpTransactionRepository.findAll().get(0);
        assertEquals(ActivityType.DAILY_CHALLENGE, tx.getActivityType());
        assertEquals(30, tx.getPoints());
        assertEquals(udc.getId(), tx.getReferenceId());
    }

    @Test
    @DisplayName("Scenario 3: Additional deposits after completion do NOT award duplicate XP")
    void scenario3_AdditionalDeposit_DoesNotAwardDuplicateXp() {
        recordContribution(new BigDecimal("10.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("10.00"));

        User refreshedUser1 = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(30, refreshedUser1.getTotalXp());
        assertEquals(1, xpTransactionRepository.count());

        // Subsequent deposit of $50
        recordContribution(new BigDecimal("50.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("50.00"));

        User refreshedUser2 = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(30, refreshedUser2.getTotalXp(), "XP must not increase on subsequent deposits");
        assertEquals(1, xpTransactionRepository.count(), "XP transactions count must stay 1");
    }

    @Test
    @DisplayName("Scenario 4: Repeated evaluation is idempotent")
    void scenario4_RepeatedEvaluation_IsIdempotent() {
        recordContribution(new BigDecimal("15.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("15.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("15.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("15.00"));

        long completedCount = userDailyChallengeRepository.findAllByUserAndChallengeDate(testUser, LocalDate.now())
                .stream().filter(u -> u.getStatus() == ChallengeStatus.COMPLETED).count();

        assertEquals(1, completedCount);
        assertEquals(1, xpTransactionRepository.count());
        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(30, refreshedUser.getTotalXp());
    }

    @Test
    @DisplayName("Scenario 5: 3 consecutive days of completion yields streak of 3")
    void scenario5_ThreeConsecutiveDays_StreakIsThree() {
        LocalDate today = LocalDate.now();
        LocalDate dayMinus2 = today.minusDays(2);
        LocalDate dayMinus1 = today.minusDays(1);

        userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(depositChallenge)
                .challengeDate(dayMinus2)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(dayMinus2.atTime(10, 0))
                .xpAwarded(30)
                .build());

        userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(depositChallenge)
                .challengeDate(dayMinus1)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(dayMinus1.atTime(10, 0))
                .xpAwarded(30)
                .build());

        // Today's completion
        recordContribution(new BigDecimal("10.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("10.00"));

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(3, streak, "Consecutive 3 days should calculate streak of 3");

        UserReward reward = userRewardRepository.findByUser(testUser).orElseThrow();
        assertEquals(3, reward.getCurrentStreak());
    }

    @Test
    @DisplayName("Scenario 6: Broken streak (Day 1 ✓, Day 2 ✓, Day 3 ✗, Day 4 ✓) yields streak of 1")
    void scenario6_BrokenStreak_CalculatesCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate dayMinus3 = today.minusDays(3);
        LocalDate dayMinus2 = today.minusDays(2);
        // dayMinus1 is missed!

        userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(depositChallenge)
                .challengeDate(dayMinus3)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(dayMinus3.atTime(10, 0))
                .xpAwarded(30)
                .build());

        userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(depositChallenge)
                .challengeDate(dayMinus2)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(dayMinus2.atTime(10, 0))
                .xpAwarded(30)
                .build());

        // Today completed
        recordContribution(new BigDecimal("10.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("10.00"));

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(1, streak, "Streak broken yesterday should restart at 1 today");
    }

    @Test
    @DisplayName("Scenario 7: Quiz attempt triggers automatic QUIZ challenge completion")
    void scenario7_QuizAttempt_CompletesQuizChallenge() {
        QuizAttempt attempt = QuizAttempt.builder()
                .user(testUser)
                .totalQuestions(5)
                .correctAnswers(5)
                .score(100.0)
                .totalXp(10)
                .isCompleted(true)
                .build();
        attempt = quizAttemptRepository.save(attempt);

        challengeCompletionService.evaluateQuizChallenges(testUser, attempt);

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, quizChallenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.COMPLETED, udc.getStatus());
        assertEquals(40, udc.getXpAwarded());

        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(40, refreshedUser.getTotalXp());
    }

    @Test
    @DisplayName("Scenario 8: Multiple distinct challenges can be completed on the same date")
    void scenario8_MultipleChallengesCompletedSameDay() {
        // Complete deposit challenge
        recordContribution(new BigDecimal("10.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("10.00"));

        // Complete manual tip challenge
        UserDailyChallenge manualUdc = userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(manualChallenge)
                .challengeDate(LocalDate.now())
                .status(ChallengeStatus.PENDING)
                .build());

        challengeCompletionService.completeManualChallenge(manualUdc.getId(), testUser);

        List<UserDailyChallenge> todayList = userDailyChallengeRepository.findAllByUserAndChallengeDate(testUser, LocalDate.now());
        assertEquals(2, todayList.stream().filter(u -> u.getStatus() == ChallengeStatus.COMPLETED).count());

        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        // 30 (deposit) + 20 (tip) = 50 XP
        assertEquals(50, refreshedUser.getTotalXp());
    }

    @Test
    @DisplayName("Scenario 9: Cannot manually complete automatically verifiable challenge")
    void scenario9_CannotManuallyCompleteVerifiableChallenge() {
        UserDailyChallenge depositUdc = userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(depositChallenge)
                .challengeDate(LocalDate.now())
                .status(ChallengeStatus.PENDING)
                .build());

        assertThrows(com.example.genz.common.exception.BadRequestException.class, () -> {
            challengeCompletionService.completeManualChallenge(depositUdc.getId(), testUser);
        });

        // Remains pending and 0 XP
        UserDailyChallenge refreshed = userDailyChallengeRepository.findById(depositUdc.getId()).orElseThrow();
        assertEquals(ChallengeStatus.PENDING, refreshed.getStatus());
        assertEquals(0, refreshed.getUser().getTotalXp());
    }

    @Test
    @DisplayName("Scenario 10: Manual completion works for TIP / MANUAL_CONFIRM challenge")
    void scenario10_ManualCompletionWorksForTip() {
        UserDailyChallenge manualUdc = userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(manualChallenge)
                .challengeDate(LocalDate.now())
                .status(ChallengeStatus.PENDING)
                .build());

        var result = challengeCompletionService.completeManualChallenge(manualUdc.getId(), testUser);
        assertEquals(ChallengeStatus.COMPLETED, result.getStatus());
        assertEquals(20, result.getXpAwarded());
        assertNotNull(result.getCompletedAt());

        User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(20, refreshedUser.getTotalXp());
    }
}
