package com.example.genz.reward;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.reward.entity.GoalMilestoneReward;
import com.example.genz.reward.enums.GoalMilestone;
import com.example.genz.reward.repository.GoalMilestoneRewardRepository;
import com.example.genz.reward.service.GoalMilestoneRewardServiceImpl;
import com.example.genz.reward.service.GoalMilestoneRewardTierService;
import com.example.genz.transaction.entity.GoalContribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoalMilestoneRewardServiceTest {

    @Mock
    private GoalMilestoneRewardRepository goalMilestoneRewardRepository;

    @Spy
    private GoalMilestoneRewardTierService tierService = new GoalMilestoneRewardTierService();

    @Mock
    private GoalMemberRepository goalMemberRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoalMilestoneRewardServiceImpl rewardService;

    private User userA;
    private User userB;
    private User userC;
    private Goal goal;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(UUID.randomUUID());
        userA.setFullName("User A");
        userA.setCoinBalance(0);

        userB = new User();
        userB.setId(UUID.randomUUID());
        userB.setFullName("User B");
        userB.setCoinBalance(0);

        userC = new User();
        userC.setId(UUID.randomUUID());
        userC.setFullName("User C");
        userC.setCoinBalance(0);

        goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setGoalName("Vacation Fund");
        goal.setTargetAmount(new BigDecimal("500"));
        goal.setOwner(userA);
    }

    @Test
    @DisplayName("1. Gradual deposit: 0% -> 25% on $500 goal awards 25 coins")
    void testGradual0To25() {
        // Target $500: 25% threshold = $125
        int earned = rewardService.processMilestoneRewards(goal, userA, BigDecimal.ZERO, new BigDecimal("125"));

        assertEquals(25, earned);
        verify(goalMilestoneRewardRepository, times(1)).save(any(GoalMilestoneReward.class));
        assertEquals(25, userA.getCoinBalance());
    }

    @Test
    @DisplayName("2. Gradual deposit: 25% -> 50% on $500 goal awards 50 coins")
    void testGradual25To50() {
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("125"), new BigDecimal("250"));

        assertEquals(50, earned);
        verify(goalMilestoneRewardRepository, times(1)).save(any(GoalMilestoneReward.class));
        assertEquals(50, userA.getCoinBalance());
    }

    @Test
    @DisplayName("3. Gradual deposit: 50% -> 75% on $500 goal awards 75 coins")
    void testGradual50To75() {
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("250"), new BigDecimal("375"));

        assertEquals(75, earned);
        verify(goalMilestoneRewardRepository, times(1)).save(any(GoalMilestoneReward.class));
        assertEquals(75, userA.getCoinBalance());
    }

    @Test
    @DisplayName("4. Gradual deposit: 75% -> 100% on $500 individual goal awards 150 coins")
    void testGradual75To100Individual() {
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_75)).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("375"), new BigDecimal("500"));

        assertEquals(150, earned);
        verify(goalMilestoneRewardRepository, times(1)).save(any(GoalMilestoneReward.class));
        assertEquals(150, userA.getCoinBalance());
    }

    @Test
    @DisplayName("5. Large deposit: 0% -> 100% in one deposit awards all 4 milestones (300 coins total)")
    void testOneDepositZeroToHundred() {
        // On $500 goal: 25 + 50 + 75 + 150 = 300 coins
        int earned = rewardService.processMilestoneRewards(goal, userA, BigDecimal.ZERO, new BigDecimal("500"));

        assertEquals(300, earned);
        verify(goalMilestoneRewardRepository, times(4)).save(any(GoalMilestoneReward.class));
        assertEquals(300, userA.getCoinBalance());
    }

    @Test
    @DisplayName("6. Deposit skips multiple milestones: 10% -> 80% on $1,000 goal awards 25%, 50%, 75% (240 coins)")
    void testTenToEightyPercent() {
        goal.setTargetAmount(new BigDecimal("1000"));
        // Tier $1,000–$1,999: 25% = 40, 50% = 80, 75% = 120, 100% = 250
        // Previous = $100 (10%), New = $800 (80%)
        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("100"), new BigDecimal("800"));

        assertEquals(40 + 80 + 120, earned);
        verify(goalMilestoneRewardRepository, times(3)).save(any(GoalMilestoneReward.class));
        assertEquals(240, userA.getCoinBalance());
    }

    @Test
    @DisplayName("7. Deposit over 100%: $450 (90%) -> $550 (110%) awards 100% milestone once")
    void testDepositOverTarget() {
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_75)).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("450"), new BigDecimal("550"));

        assertEquals(150, earned);
        verify(goalMilestoneRewardRepository, times(1)).save(any(GoalMilestoneReward.class));
    }

    @Test
    @DisplayName("8. Repeated deposit after 100%: $550 -> $650 awards 0 additional milestone rewards")
    void testRepeatedDepositAfterHundred() {
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(any(), any())).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("550"), new BigDecimal("650"));

        assertEquals(0, earned);
        verify(goalMilestoneRewardRepository, never()).save(any(GoalMilestoneReward.class));
    }

    @Test
    @DisplayName("9. Duplicate prevention: milestone already rewarded is not awarded again")
    void testAlreadyAwardedMilestoneNotRepeated() {
        // Goal at $100, user deposits $50 to reach $150 (crossing 25% = $125). But 25% was already recorded.
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);

        int earned = rewardService.processMilestoneRewards(goal, userA, new BigDecimal("100"), new BigDecimal("150"));

        assertEquals(0, earned);
        verify(goalMilestoneRewardRepository, never()).save(any(GoalMilestoneReward.class));
    }

    @Test
    @DisplayName("10. Shared goal: Different members cross 25%, 50%, 75% - each receives their respective milestone")
    void testSharedGoalDifferentMembersIntermediateMilestones() {
        goal.setTargetAmount(new BigDecimal("1000"));
        // Tier $1,000: 25%=40, 50%=80, 75%=120, 100%=250

        // Step 1: Member A deposits $250 (0% -> 25%)
        int aEarned = rewardService.processMilestoneRewards(goal, userA, BigDecimal.ZERO, new BigDecimal("250"));
        assertEquals(40, aEarned);
        assertEquals(40, userA.getCoinBalance());

        // Step 2: Member B deposits $250 (25% -> 50%)
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        int bEarned = rewardService.processMilestoneRewards(goal, userB, new BigDecimal("250"), new BigDecimal("500"));
        assertEquals(80, bEarned);
        assertEquals(80, userB.getCoinBalance());

        // Step 3: Member C deposits $250 (50% -> 75%)
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        int cEarned = rewardService.processMilestoneRewards(goal, userC, new BigDecimal("500"), new BigDecimal("750"));
        assertEquals(120, cEarned);
        assertEquals(120, userC.getCoinBalance());
    }

    @Test
    @DisplayName("11. Shared goal: Proportional 100% completion distribution with Largest Remainder rounding")
    void testSharedGoalProportionalDistributionWithRounding() {
        // Target $500, Tier $500-$999: 100% reward = 150 coins
        // A contributed $250 (50%), B contributed $125 (25%), C contributed $125 (25%)
        when(goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE)).thenReturn(3L);

        GoalMember memberA = new GoalMember(UUID.randomUUID(), goal, userA, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalMember memberB = new GoalMember(UUID.randomUUID(), goal, userB, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalMember memberC = new GoalMember(UUID.randomUUID(), goal, userC, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now(), null);

        GoalContribution cA = new GoalContribution(UUID.randomUUID(), goal, memberA, new BigDecimal("250"), "deposit", LocalDateTime.now());
        GoalContribution cB = new GoalContribution(UUID.randomUUID(), goal, memberB, new BigDecimal("125"), "deposit", LocalDateTime.now());
        GoalContribution cC = new GoalContribution(UUID.randomUUID(), goal, memberC, new BigDecimal("125"), "deposit", LocalDateTime.now());

        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId()))
                .thenReturn(List.of(cA, cB, cC));

        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_75)).thenReturn(true);

        // User C makes the deposit taking goal from $375 (75%) to $500 (100%)
        int userCEarned = rewardService.processMilestoneRewards(goal, userC, new BigDecimal("375"), new BigDecimal("500"));

        ArgumentCaptor<GoalMilestoneReward> captor = ArgumentCaptor.forClass(GoalMilestoneReward.class);
        verify(goalMilestoneRewardRepository, times(3)).save(captor.capture());

        List<GoalMilestoneReward> savedRewards = captor.getAllValues();
        int totalCoinsDistributed = savedRewards.stream().mapToInt(GoalMilestoneReward::getCoinsEarned).sum();

        // Exactly 150 coins distributed!
        assertEquals(150, totalCoinsDistributed);

        // A (50%) gets 75 coins
        assertEquals(75, userA.getCoinBalance());

        // B and C (25% each): one gets 38 and other gets 37, total = 75
        assertEquals(75, userB.getCoinBalance() + userC.getCoinBalance());
        assertTrue(userB.getCoinBalance() == 37 || userB.getCoinBalance() == 38);
        assertTrue(userC.getCoinBalance() == 37 || userC.getCoinBalance() == 38);
    }

    @Test
    @DisplayName("12. Shared goal: Member with 0 contribution receives 0 completion reward")
    void testSharedGoalZeroContributionMemberReceivesZero() {
        goal.setTargetAmount(new BigDecimal("1000"));
        // 100% reward = 250 coins
        // A contributed $500, B contributed $500, C contributed $0
        when(goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE)).thenReturn(3L);

        GoalMember memberA = new GoalMember(UUID.randomUUID(), goal, userA, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalMember memberB = new GoalMember(UUID.randomUUID(), goal, userB, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now(), null);

        GoalContribution cA = new GoalContribution(UUID.randomUUID(), goal, memberA, new BigDecimal("500"), "deposit", LocalDateTime.now());
        GoalContribution cB = new GoalContribution(UUID.randomUUID(), goal, memberB, new BigDecimal("500"), "deposit", LocalDateTime.now());

        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId()))
                .thenReturn(List.of(cA, cB));

        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_75)).thenReturn(true);

        rewardService.processMilestoneRewards(goal, userB, new BigDecimal("500"), new BigDecimal("1000"));

        assertEquals(125, userA.getCoinBalance());
        assertEquals(125, userB.getCoinBalance());
        assertEquals(0, userC.getCoinBalance());
    }

    @Test
    @DisplayName("13. Shared goal: Only one contributing member receives 100% of completion reward")
    void testSharedGoalOnlyOneContributingMember() {
        goal.setTargetAmount(new BigDecimal("1000"));
        // 100% reward = 250 coins
        // Multiple members in goal, but only A contributed $1,000
        when(goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE)).thenReturn(3L);

        GoalMember memberA = new GoalMember(UUID.randomUUID(), goal, userA, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalContribution cA = new GoalContribution(UUID.randomUUID(), goal, memberA, new BigDecimal("1000"), "deposit", LocalDateTime.now());

        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId()))
                .thenReturn(List.of(cA));

        // Member A deposits $1,000 in one go (0% -> 100%)
        int aEarned = rewardService.processMilestoneRewards(goal, userA, BigDecimal.ZERO, new BigDecimal("1000"));

        // A receives all milestone rewards: 40 + 80 + 120 + 250 = 490
        assertEquals(490, aEarned);
        assertEquals(490, userA.getCoinBalance());
        assertEquals(0, userB.getCoinBalance());
        assertEquals(0, userC.getCoinBalance());
    }

    @Test
    @DisplayName("14. Shared goal: Uneven contributions (A=25%, B=15%, C=60%) with 1,000 coin reward")
    void testSharedGoalUnevenContributions() {
        goal.setTargetAmount(new BigDecimal("20000"));
        // Tier $20,000+: 100% reward = 1,000 coins
        // A contributed $500 (25%), B contributed $300 (15%), C contributed $1,200 (60%)
        // Total contribution = $2,000
        when(goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE)).thenReturn(3L);

        GoalMember memberA = new GoalMember(UUID.randomUUID(), goal, userA, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalMember memberB = new GoalMember(UUID.randomUUID(), goal, userB, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now(), null);
        GoalMember memberC = new GoalMember(UUID.randomUUID(), goal, userC, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now(), null);

        GoalContribution cA = new GoalContribution(UUID.randomUUID(), goal, memberA, new BigDecimal("500"), "deposit", LocalDateTime.now());
        GoalContribution cB = new GoalContribution(UUID.randomUUID(), goal, memberB, new BigDecimal("300"), "deposit", LocalDateTime.now());
        GoalContribution cC = new GoalContribution(UUID.randomUUID(), goal, memberC, new BigDecimal("1200"), "deposit", LocalDateTime.now());

        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId()))
                .thenReturn(List.of(cA, cB, cC));

        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_25)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_50)).thenReturn(true);
        when(goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), GoalMilestone.MILESTONE_75)).thenReturn(true);

        rewardService.processMilestoneRewards(goal, userC, new BigDecimal("15000"), new BigDecimal("20000"));

        assertEquals(250, userA.getCoinBalance()); // 25% of 1000 = 250
        assertEquals(150, userB.getCoinBalance()); // 15% of 1000 = 150
        assertEquals(600, userC.getCoinBalance()); // 60% of 1000 = 600
        assertEquals(1000, userA.getCoinBalance() + userB.getCoinBalance() + userC.getCoinBalance());
    }
}
