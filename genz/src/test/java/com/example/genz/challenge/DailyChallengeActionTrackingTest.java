package com.example.genz.challenge;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.goal.dto.CreateGoalRequest;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.service.GoalService;
import com.example.genz.invitation.dto.CreateInvitationRequest;
import com.example.genz.invitation.dto.InvitationResponse;
import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import com.example.genz.invitation.repository.GoalInvitationRepository;
import com.example.genz.invitation.service.GoalInvitationService;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.learning.repository.QuizAttemptRepository;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.transaction.entity.GoalContribution;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_action_tracking_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class DailyChallengeActionTrackingTest {

    @Autowired
    private ChallengeCompletionService challengeCompletionService;

    @Autowired
    private DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Autowired
    private GoalInvitationRepository goalInvitationRepository;

    @Autowired
    private GoalService goalService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private GoalInvitationService goalInvitationService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;
    private User friendUser;
    private Goal testGoal;
    private GoalMember testGoalMember;
    private GrouplWallet testWallet;

    @BeforeEach
    void setUp() {
        userDailyChallengeRepository.deleteAll();
        dailyChallengeRepository.deleteAll();
        goalContributionRepository.deleteAll();
        goalInvitationRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        xpTransactionRepository.deleteAll();
        goalWalletRepository.deleteAll();
        goalMemberRepository.deleteAll();
        goalRepository.deleteAll();

        testUser = new User();
        testUser.setFullName("Trackable User");
        testUser.setPhoneNumber("0911111111");
        testUser.setGender(Gender.MALE);
        testUser.setStatus("ACTIVE");
        testUser.setDateOfBirth(LocalDate.of(2000, 1, 1));
        testUser.setCoinBalance(100);
        testUser.setTotalXp(0);
        testUser = userRepository.save(testUser);

        friendUser = new User();
        friendUser.setFullName("Friend User");
        friendUser.setPhoneNumber("0922222222");
        friendUser.setGender(Gender.FEMALE);
        friendUser.setStatus("ACTIVE");
        friendUser.setDateOfBirth(LocalDate.of(2001, 2, 2));
        friendUser.setCoinBalance(50);
        friendUser.setTotalXp(0);
        friendUser = userRepository.save(friendUser);

        testGoal = new Goal();
        testGoal.setGoalName("New Laptop");
        testGoal.setTargetAmount(new BigDecimal("1000.00"));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setCategory(GoalCategory.TECH);
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

        testWallet = new GrouplWallet();
        testWallet.setGoal(testGoal);
        testWallet.setUser(testUser);
        testWallet.setBalance(BigDecimal.ZERO);
        testWallet.setCurrency(com.example.genz.transaction.enums.Currency.USD);
        testWallet.setCreatedAt(LocalDateTime.now());
        testWallet = goalWalletRepository.save(testWallet);
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
    @DisplayName("Deposit: $1.00 + $1.00 completes $2.00 challenge and awards coins once")
    void deposit_OneDollarPlusOneDollar_CompletesTwoDollarChallenge() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Save at least $2 today")
                .description("Deposit at least $2 into any goal.")
                .challengeType(ChallengeType.DEPOSIT_AMOUNT)
                .targetValue(new BigDecimal("2.00"))
                .rewardCoins(25)
                .xpReward(20)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        // First deposit: $1.00
        recordContribution(new BigDecimal("1.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("1.00"));

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.IN_PROGRESS, udc.getStatus());
        assertEquals(0, new BigDecimal("1.00").compareTo(udc.getProgress()));
        assertEquals(0, udc.getRewardCoinsAwarded());

        User userAfterFirst = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(100, userAfterFirst.getCoinBalance()); // No reward yet

        // Second deposit: $1.00 -> total $2.00 >= $2.00
        recordContribution(new BigDecimal("1.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("1.00"));

        UserDailyChallenge udcCompleted = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.COMPLETED, udcCompleted.getStatus());
        assertEquals(0, new BigDecimal("2.00").compareTo(udcCompleted.getProgress()));
        assertEquals(25, udcCompleted.getRewardCoinsAwarded());
        assertNotNull(udcCompleted.getCompletedAt());

        User userAfterSecond = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(125, userAfterSecond.getCoinBalance(), "Coins balance should increase by 25");

        // Third deposit: $1.00 -> already completed, coins should NOT be awarded again
        recordContribution(new BigDecimal("1.00"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("1.00"));

        User userAfterThird = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(125, userAfterThird.getCoinBalance(), "Subsequent deposit should NOT award duplicate coins");
    }

    @Test
    @DisplayName("Deposit: $1.99 does not complete a $2.00 challenge")
    void deposit_OneDollarNinetyNineCents_DoesNotComplete() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Save at least $2 today")
                .challengeType(ChallengeType.DEPOSIT_AMOUNT)
                .targetValue(new BigDecimal("2.00"))
                .rewardCoins(25)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        recordContribution(new BigDecimal("1.99"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("1.99"));

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.IN_PROGRESS, udc.getStatus());
        assertEquals(0, new BigDecimal("1.99").compareTo(udc.getProgress()));
        assertEquals(0, udc.getRewardCoinsAwarded());
        assertNull(udc.getCompletedAt());

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(100, refreshed.getCoinBalance());
    }

    @Test
    @DisplayName("Deposit: DEPOSIT_ANY completes when any successful deposit exists")
    void deposit_AnyDeposit_CompletesChallenge() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Make a deposit today")
                .challengeType(ChallengeType.DEPOSIT_ANY)
                .targetValue(new BigDecimal("1.00"))
                .rewardCoins(15)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        recordContribution(new BigDecimal("0.50"));
        challengeCompletionService.evaluateDepositChallenges(testUser, new BigDecimal("0.50"));

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.COMPLETED, udc.getStatus());
        assertEquals(15, udc.getRewardCoinsAwarded());

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(115, refreshed.getCoinBalance());
    }

    @Test
    @DisplayName("Goal: Successful goal creation triggers and completes CREATE_GOAL challenge")
    void goal_Creation_CompletesGoalChallenge() {
        // Clear existing goals to test fresh goal creation count
        goalContributionRepository.deleteAll();
        goalWalletRepository.deleteAll();
        goalMemberRepository.deleteAll();
        goalRepository.deleteAll();

        DailyChallenge challenge = DailyChallenge.builder()
                .title("Create a saving goal")
                .challengeType(ChallengeType.CREATE_GOAL)
                .targetValue(new BigDecimal("1.00"))
                .rewardCoins(20)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        CreateGoalRequest request = new CreateGoalRequest();
        request.setGoalName("Vacation Fund");
        request.setTargetAmount(new BigDecimal("500.00"));
        request.setTargetDate(LocalDate.now().plusMonths(3));
        request.setCategory(GoalCategory.TRAVEL);
        request.setCurrency(com.example.genz.transaction.enums.Currency.USD);

        goalService.createGoal(request, testUser.getPhoneNumber());

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.COMPLETED, udc.getStatus());
        assertEquals(0, BigDecimal.ONE.compareTo(udc.getProgress()));
        assertEquals(20, udc.getRewardCoinsAwarded());

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(120, refreshed.getCoinBalance());
    }

    @Test
    @DisplayName("Quiz: Completed quiz triggers and completes COMPLETE_QUIZ challenge")
    void quiz_Completion_CompletesQuizChallenge() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Complete a quiz")
                .challengeType(ChallengeType.COMPLETE_QUIZ)
                .targetValue(new BigDecimal("1.00"))
                .rewardCoins(20)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        // Uncompleted quiz attempt does NOT complete challenge
        QuizAttempt uncompleted = QuizAttempt.builder()
                .user(testUser)
                .totalQuestions(5)
                .correctAnswers(2)
                .isCompleted(false)
                .startedAt(LocalDateTime.now())
                .build();
        uncompleted = quizAttemptRepository.save(uncompleted);
        challengeCompletionService.evaluateQuizChallenges(testUser, uncompleted);

        assertTrue(userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now()).isEmpty());

        // Completed quiz attempt completes challenge
        QuizAttempt completed = QuizAttempt.builder()
                .user(testUser)
                .totalQuestions(5)
                .correctAnswers(5)
                .isCompleted(true)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        completed = quizAttemptRepository.save(completed);
        challengeCompletionService.evaluateQuizChallenges(testUser, completed);

        UserDailyChallenge udc = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();

        assertEquals(ChallengeStatus.COMPLETED, udc.getStatus());
        assertEquals(20, udc.getRewardCoinsAwarded());

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(120, refreshed.getCoinBalance());
    }

    @Test
    @DisplayName("XP: Earn XP challenge accumulates points and completes at threshold")
    void xp_Earning_CompletesEarnXpChallenge() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Earn 50 XP today")
                .challengeType(ChallengeType.EARN_XP)
                .targetValue(new BigDecimal("50.00"))
                .rewardCoins(25)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        // Earn 20 XP
        XpTransaction tx1 = XpTransaction.builder()
                .user(testUser)
                .points(20)
                .activityType(ActivityType.QUIZ)
                .build();
        xpTransactionRepository.save(tx1);
        challengeCompletionService.evaluateXpChallenges(testUser, 20);

        UserDailyChallenge udc1 = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.IN_PROGRESS, udc1.getStatus());
        assertEquals(0, new BigDecimal("20.00").compareTo(udc1.getProgress()));

        // Earn 35 XP -> total 55 XP >= 50 XP
        XpTransaction tx2 = XpTransaction.builder()
                .user(testUser)
                .points(35)
                .activityType(ActivityType.LESSON)
                .build();
        xpTransactionRepository.save(tx2);
        challengeCompletionService.evaluateXpChallenges(testUser, 35);

        UserDailyChallenge udcCompleted = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, challenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.COMPLETED, udcCompleted.getStatus());
        assertEquals(0, new BigDecimal("55.00").compareTo(udcCompleted.getProgress()));
        assertEquals(25, udcCompleted.getRewardCoinsAwarded());

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(125, refreshed.getCoinBalance());
    }

    @Test
    @DisplayName("Invitation: INVITE_FRIEND and FRIEND_JOINED complete on verified events")
    void invitation_InviteAndFriendJoined_CompleteChallenges() {
        DailyChallenge inviteChallenge = DailyChallenge.builder()
                .title("Invite 1 friend")
                .challengeType(ChallengeType.INVITE_FRIEND)
                .targetValue(new BigDecimal("1.00"))
                .rewardCoins(30)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(inviteChallenge);

        DailyChallenge friendJoinedChallenge = DailyChallenge.builder()
                .title("1 Friend Joined")
                .challengeType(ChallengeType.FRIEND_JOINED)
                .targetValue(new BigDecimal("1.00"))
                .rewardCoins(40)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(friendJoinedChallenge);

        // Step 1: Send invitation
        CreateInvitationRequest inviteRequest = new CreateInvitationRequest();
        inviteRequest.setInviteeId(friendUser.getId());
        InvitationResponse inviteResponse = goalInvitationService.inviteUser(testGoal.getId(), inviteRequest, testUser.getPhoneNumber());

        UserDailyChallenge udcInvite = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, inviteChallenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.COMPLETED, udcInvite.getStatus());
        assertEquals(30, udcInvite.getRewardCoinsAwarded());

        // FRIEND_JOINED should not be completed yet
        assertTrue(userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(testUser, friendJoinedChallenge, LocalDate.now()).isEmpty()
                || userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(testUser, friendJoinedChallenge, LocalDate.now()).get().getStatus() != ChallengeStatus.COMPLETED);

        // Step 2: Friend accepts invitation
        goalInvitationService.acceptInvitation(inviteResponse.getInvitationId(), friendUser.getPhoneNumber());

        UserDailyChallenge udcJoined = userDailyChallengeRepository
                .findByUserAndChallengeAndChallengeDate(testUser, friendJoinedChallenge, LocalDate.now())
                .orElseThrow();
        assertEquals(ChallengeStatus.COMPLETED, udcJoined.getStatus());
        assertEquals(40, udcJoined.getRewardCoinsAwarded());

        User refreshedInviter = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(170, refreshedInviter.getCoinBalance(), "100 + 30 (invite) + 40 (joined) = 170 coins");
    }

    @Test
    @DisplayName("Daily boundary: Yesterday's deposits do not count toward today's challenge")
    void dailyBoundary_YesterdayActivity_DoesNotCountToday() {
        DailyChallenge challenge = DailyChallenge.builder()
                .title("Save at least $2 today")
                .challengeType(ChallengeType.DEPOSIT_AMOUNT)
                .targetValue(new BigDecimal("2.00"))
                .rewardCoins(25)
                .isActive(true)
                .build();
        dailyChallengeRepository.save(challenge);

        // Record a contribution with yesterday's date
        GoalContribution yesterdayContribution = new GoalContribution();
        yesterdayContribution.setGoal(testGoal);
        yesterdayContribution.setGoalMember(testGoalMember);
        yesterdayContribution.setAmount(new BigDecimal("5.00"));
        yesterdayContribution = goalContributionRepository.saveAndFlush(yesterdayContribution);

        entityManager.createNativeQuery("UPDATE goal_contributions SET contributed_at = :yesterday WHERE contribution_id = :id")
                .setParameter("yesterday", LocalDateTime.now().minusDays(1))
                .setParameter("id", yesterdayContribution.getId())
                .executeUpdate();
        entityManager.clear();

        challengeCompletionService.evaluateDepositChallenges(testUser, BigDecimal.ZERO);

        List<UserDailyChallenge> todayList = userDailyChallengeRepository.findAllByUserAndChallengeDate(testUser, LocalDate.now());
        // Progress for today must be 0 and challenge NOT completed
        assertTrue(todayList.isEmpty() || todayList.get(0).getStatus() != ChallengeStatus.COMPLETED);
        if (!todayList.isEmpty()) {
            assertEquals(0, BigDecimal.ZERO.compareTo(todayList.get(0).getProgress()));
        }

        User refreshed = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(100, refreshed.getCoinBalance());
    }
}
