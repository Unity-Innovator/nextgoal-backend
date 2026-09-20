package com.example.genz.goal;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.GoalType;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.reward.entity.GoalMilestoneReward;
import com.example.genz.reward.enums.GoalMilestone;
import com.example.genz.reward.repository.GoalMilestoneRewardRepository;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_milestone_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@Transactional
class GoalMilestoneDepositIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private GoalMilestoneRewardRepository goalMilestoneRewardRepository;

    @Autowired
    private com.example.genz.challenge.repository.DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private com.example.genz.challenge.repository.UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User owner;
    private User memberB;
    private Goal goal;
    private GrouplWallet wallet;

    @BeforeEach
    void setUp() {
        userDailyChallengeRepository.deleteAll();
        dailyChallengeRepository.deleteAll();

        owner = new User();
        owner.setFullName("Alice Owner");
        owner.setPhoneNumber("0981112223");
        owner.setDateOfBirth(LocalDate.of(2000, 1, 1));
        owner.setGender(Gender.FEMALE);
        owner.setStatus("ACTIVE");
        owner.setRole(UserRole.USER);
        owner.setCoinBalance(0);
        owner = userRepository.saveAndFlush(owner);

        memberB = new User();
        memberB.setFullName("Bob Member");
        memberB.setPhoneNumber("0982223334");
        memberB.setDateOfBirth(LocalDate.of(2001, 2, 2));
        memberB.setGender(Gender.MALE);
        memberB.setStatus("ACTIVE");
        memberB.setRole(UserRole.USER);
        memberB.setCoinBalance(0);
        memberB = userRepository.saveAndFlush(memberB);

        goal = new Goal();
        goal.setGoalName("New Laptop");
        goal.setTargetAmount(new BigDecimal("500.00"));
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setRemaining(new BigDecimal("500.00"));
        goal.setTargetDate(LocalDate.now().plusMonths(3));
        goal.setCategory(GoalCategory.TECH);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setGoalType(GoalType.FLEXIBLE);
        goal.setOwner(owner);
        goal.setCreatedAt(LocalDateTime.now());
        goal = goalRepository.saveAndFlush(goal);

        wallet = new GrouplWallet();
        wallet.setGoal(goal);
        wallet.setUser(owner);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency(Currency.USD);
        wallet = goalWalletRepository.saveAndFlush(wallet);

        GoalMember ownerMember = new GoalMember();
        ownerMember.setGoal(goal);
        ownerMember.setUser(owner);
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(ownerMember);
    }

    @Test
    @DisplayName("End-to-End Individual Goal Deposit: 0% -> 100% awards 300 coins and updates DB state")
    void testIndividualGoalFullDepositEndToEnd() {
        DepositRequest request = new DepositRequest(goal.getId(), new BigDecimal("500.00"));

        DepositResponse response = walletService.deposit(request, owner.getPhoneNumber());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(300, response.getCoinsEarned());
        assertEquals(new BigDecimal("500.00"), response.getCurrentAmount());
        assertEquals(new BigDecimal("500.00"), response.getTargetAmount());
        assertEquals(100.0, response.getProgressPercentage());
        assertEquals(GoalStatus.COMPLETED, response.getGoalStatus());

        // Verify Data block
        assertNotNull(response.getData());
        assertEquals(300, response.getData().getCoinsEarned());
        assertEquals(GoalStatus.COMPLETED, response.getData().getGoalStatus());

        // Verify DB records
        List<GoalMilestoneReward> rewards = goalMilestoneRewardRepository.findByGoalId(goal.getId());
        assertEquals(4, rewards.size());

        // Verify user coin balance updated in database
        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertEquals(300, updatedOwner.getCoinBalance());

        // Verify goal status in database
        Goal updatedGoal = goalRepository.findById(goal.getId()).orElseThrow();
        assertEquals(GoalStatus.COMPLETED, updatedGoal.getStatus());
        assertNotNull(updatedGoal.getCompletedAt());
    }

    @Test
    @DisplayName("End-to-End Shared Goal: Multi-member deposits crossing milestones and proportional completion")
    void testSharedGoalMultiMemberDepositEndToEnd() {
        // Add memberB to goal to make it a shared goal
        GoalMember member = new GoalMember();
        member.setGoal(goal);
        member.setUser(memberB);
        member.setRole(MemberRole.MEMBER);
        member.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(member);

        // Step 1: Owner deposits $250 (50% progress, crosses 25% and 50%)
        // Tier $500: 25%=25, 50%=50 -> 75 coins
        DepositRequest req1 = new DepositRequest(goal.getId(), new BigDecimal("250.00"));

        DepositResponse res1 = walletService.deposit(req1, owner.getPhoneNumber());
        assertEquals(75, res1.getCoinsEarned());
        assertEquals(new BigDecimal("250.00"), res1.getCurrentAmount());
        assertEquals(50.0, res1.getProgressPercentage());
        assertEquals(GoalStatus.ACTIVE, res1.getGoalStatus());

        User ownerAfterStep1 = userRepository.findById(owner.getId()).orElseThrow();
        assertEquals(75, ownerAfterStep1.getCoinBalance());

        // Step 2: Member B deposits $250 (takes goal from 50% to 100%, crosses 75% and 100%)
        // 75% intermediate goes to member B: 75 coins
        // 100% completion reward is 150 coins: Owner contributed $250 (50%), Member B contributed $250 (50%)
        // Owner gets 75 coins, Member B gets 75 coins
        // Total coins for Member B from this deposit: 75 (75%) + 75 (100%) = 150 coins
        DepositRequest req2 = new DepositRequest(goal.getId(), new BigDecimal("250.00"));

        DepositResponse res2 = walletService.deposit(req2, memberB.getPhoneNumber());
        assertEquals(150, res2.getCoinsEarned());
        assertEquals(new BigDecimal("500.00"), res2.getCurrentAmount());
        assertEquals(100.0, res2.getProgressPercentage());
        assertEquals(GoalStatus.COMPLETED, res2.getGoalStatus());

        // Verify final coin balances in database
        User finalOwner = userRepository.findById(owner.getId()).orElseThrow();
        User finalMemberB = userRepository.findById(memberB.getId()).orElseThrow();

        // Owner: 75 (from 25%+50%) + 75 (from 100% completion) = 150
        assertEquals(150, finalOwner.getCoinBalance());
        // Member B: 75 (from 75%) + 75 (from 100% completion) = 150
        assertEquals(150, finalMemberB.getCoinBalance());
    }

    @Test
    @DisplayName("Transaction Atomicity: If transaction rolls back, neither deposit nor rewards are committed")
    void testTransactionAtomicityRollback() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            DepositRequest req = new DepositRequest(goal.getId(), new BigDecimal("500.00"));
            walletService.deposit(req, owner.getPhoneNumber());

            // Deliberately roll back
            transactionManager.rollback(status);
        } catch (Exception e) {
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }
        }

        // Verify DB did NOT persist the deposit or rewards
        List<GoalMilestoneReward> rewards = goalMilestoneRewardRepository.findByGoalId(goal.getId());
        assertTrue(rewards.isEmpty(), "Rewards should not be saved after rollback");

        Goal freshGoal = goalRepository.findById(goal.getId()).orElseThrow();
        assertEquals(BigDecimal.ZERO.compareTo(freshGoal.getCurrentAmount()), 0);
        assertEquals(GoalStatus.ACTIVE, freshGoal.getStatus());

        User freshOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertEquals(0, freshOwner.getCoinBalance());
    }
}
