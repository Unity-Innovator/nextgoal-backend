package com.example.genz.challenge;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_streak_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class StreakResetLogicIntegrationTest {

    @Autowired
    private ChallengeCompletionService challengeCompletionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    private User testUser;
    private Goal testGoal;
    private GoalMember testMember;
    private DailyChallenge testChallenge;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .phoneNumber("099999999")
                .fullName("Streak Tester")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build());

        testGoal = new Goal();
        testGoal.setGoalName("Vacation Fund");
        testGoal.setTargetAmount(new BigDecimal("1000.00"));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setTargetDate(LocalDate.now().plusMonths(6));
        testGoal.setCategory(GoalCategory.TRAVEL);
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setOwner(testUser);
        testGoal.setCreatedAt(LocalDateTime.now().minusDays(10));
        testGoal = goalRepository.save(testGoal);

        testMember = new GoalMember();
        testMember.setGoal(testGoal);
        testMember.setUser(testUser);
        testMember.setRole(MemberRole.OWNER);
        testMember.setStatus(MemberStatus.ACTIVE);
        testMember = goalMemberRepository.save(testMember);

        GrouplWallet wallet = new GrouplWallet();
        wallet.setGoal(testGoal);
        wallet.setUser(testUser);
        wallet.setCurrency(Currency.USD);
        wallet.setBalance(BigDecimal.ZERO);
        goalWalletRepository.save(wallet);

        testChallenge = dailyChallengeRepository.save(DailyChallenge.builder()
                .title("Save $5")
                .description("Save at least $5 today")
                .challengeType(ChallengeType.DEPOSIT_ANY)
                .xpReward(20)
                .rewardCoins(5)
                .targetValue(new BigDecimal("5.00"))
                .isActive(true)
                .build());
    }

    private void recordPastContribution(LocalDate date, BigDecimal amount) {
        GoalContribution gc = new GoalContribution();
        gc.setGoal(testGoal);
        gc.setGoalMember(testMember);
        gc.setAmount(amount);
        gc.setContributedAt(date.atTime(12, 0));
        goalContributionRepository.save(gc);
    }

    private void recordPastChallengeCompletion(LocalDate date) {
        userDailyChallengeRepository.save(UserDailyChallenge.builder()
                .user(testUser)
                .challenge(testChallenge)
                .challengeDate(date)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(date.atTime(14, 0))
                .xpAwarded(20)
                .build());
    }

    @Test
    @DisplayName("Scenario 1: Consecutive days -> Day 1 activity, Day 2 activity, Day 3 activity yields streak of 3")
    void testConsecutiveDaysIncrementsStreak() {
        LocalDate today = LocalDate.now();
        recordPastContribution(today.minusDays(2), new BigDecimal("10.00"));
        recordPastChallengeCompletion(today.minusDays(1));
        recordPastContribution(today, new BigDecimal("10.00"));

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(3, streak, "Three consecutive active calendar days must yield streak of 3");

        // Calling updateStreak stores streak of 3
        int updatedStreak = challengeCompletionService.updateStreak(testUser);
        assertEquals(3, updatedStreak);

        UserReward reward = userRewardRepository.findByUser(testUser).orElseThrow();
        assertEquals(3, reward.getCurrentStreak());
        assertEquals(today, reward.getLastActivityDate());
    }

    @Test
    @DisplayName("Scenario 2: Same-day multiple activities -> 2 deposits and 1 challenge completion on same day yields streak of 1")
    void testSameDayMultipleActivitiesIncrementsOnce() {
        LocalDate today = LocalDate.now();

        // 2 deposits on same day
        recordPastContribution(today, new BigDecimal("10.00"));
        recordPastContribution(today, new BigDecimal("25.00"));

        // 1 challenge on same day
        recordPastChallengeCompletion(today);

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(1, streak, "Multiple qualifying activities on the same day must not increment streak multiple times");
    }

    @Test
    @DisplayName("Scenario 3: Missed day -> Day 1 activity, Day 2 activity, Day 3 missed, Day 4 activity yields streak of 1")
    void testMissedDayResetsStreakToOne() {
        LocalDate today = LocalDate.now();
        // Day -3: active
        recordPastContribution(today.minusDays(3), new BigDecimal("10.00"));
        // Day -2: active
        recordPastContribution(today.minusDays(2), new BigDecimal("10.00"));
        // Day -1 (yesterday): MISSED! No activity
        // Day 0 (today): active
        recordPastContribution(today, new BigDecimal("10.00"));

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(1, streak, "When yesterday was missed, streak must reset and restart at 1 today");

        challengeCompletionService.updateStreak(testUser);
        UserReward reward = userRewardRepository.findByUser(testUser).orElseThrow();
        assertEquals(1, reward.getCurrentStreak());
    }

    @Test
    @DisplayName("Scenario 4: Current day has not been completed yet -> Yesterday had activity, Today has no activity yet -> streak remains available")
    void testCurrentDayIncompleteStreakNotBrokenYet() {
        LocalDate today = LocalDate.now();
        // Day -2: active
        recordPastContribution(today.minusDays(2), new BigDecimal("10.00"));
        // Day -1 (yesterday): active
        recordPastContribution(today.minusDays(1), new BigDecimal("10.00"));
        // Today: no activity yet!

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(2, streak, "Streak should not be prematurely broken if today has not completed yet");
    }

    @Test
    @DisplayName("Scenario 5: Missed-day behavior when reading streak -> Yesterday was missed and no activity today -> streak is 0")
    void testMissedDayReadingStreakReturnsZero() {
        LocalDate today = LocalDate.now();
        // Active 2 days ago, but missed yesterday and no activity today
        recordPastContribution(today.minusDays(2), new BigDecimal("10.00"));

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(0, streak, "Streak must be broken (0) when a full calendar day was missed");
    }

    @Test
    @DisplayName("Scenario 6: Daily challenge completion counts toward streak")
    void testDailyChallengeCountsTowardStreak() {
        LocalDate today = LocalDate.now();
        recordPastChallengeCompletion(today.minusDays(1));
        recordPastChallengeCompletion(today);

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(2, streak, "Daily challenge completion must count as a qualifying streak activity");
    }

    @Test
    @DisplayName("Scenario 7: Deposit counts toward streak via WalletService.deposit")
    void testDepositCountsTowardStreak() {
        LocalDate today = LocalDate.now();
        // Day 1: deposit yesterday
        recordPastContribution(today.minusDays(1), new BigDecimal("20.00"));

        // Day 2: deposit today through WalletService
        DepositRequest request = new DepositRequest(testGoal.getId(), new BigDecimal("15.00"));
        walletService.deposit(request, testUser.getPhoneNumber());

        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(2, streak, "Successful deposit must continue streak to 2");

        UserReward reward = userRewardRepository.findByUser(testUser).orElseThrow();
        assertEquals(2, reward.getCurrentStreak());
    }

    @Test
    @DisplayName("Scenario 8: Failed deposit does not count toward streak")
    void testFailedDepositDoesNotCountTowardStreak() {
        LocalDate today = LocalDate.now();
        // Day -2: successful deposit
        recordPastContribution(today.minusDays(2), new BigDecimal("20.00"));

        // Day -1 (yesterday): deposit failed (e.g. invalid goal ID)
        DepositRequest badRequest = new DepositRequest(UUID.randomUUID(), new BigDecimal("10.00"));
        assertThrows(Exception.class, () -> walletService.deposit(badRequest, testUser.getPhoneNumber()));

        // Day 0 (today): successful deposit
        DepositRequest validRequest = new DepositRequest(testGoal.getId(), new BigDecimal("10.00"));
        walletService.deposit(validRequest, testUser.getPhoneNumber());

        // Because yesterday's deposit failed, yesterday was missed!
        int streak = challengeCompletionService.calculateStreak(testUser);
        assertEquals(1, streak, "Failed deposit must not count, so streak resets to 1 after missed day");
    }
}
