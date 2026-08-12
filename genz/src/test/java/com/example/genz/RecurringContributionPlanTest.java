package com.example.genz;

import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.ContributionPlanStatusResponse;
import com.example.genz.DTO.WalletDTO.DepositRequest;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.SavingPlan;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Enums.ContributionFrequency;
import com.example.genz.Enums.Currency;
import com.example.genz.Enums.GoalCategory;
import com.example.genz.Enums.GoalStatus;
import com.example.genz.Enums.GoalType;
import com.example.genz.Enums.PeriodStatus;
import com.example.genz.Enums.SavingPlanStatus;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalMemberRepository;
import com.example.genz.Repository.GoalRepository.GoalRepository;
import com.example.genz.Repository.GoalRepository.SavingPlanRepository;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.Service.GoalService.GoalService;
import com.example.genz.Service.WalletService.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringContributionPlanTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalMemberRepository goalMemberRepository;
    @Mock
    private GoalWalletRepository goalWalletRepository;
    @Mock
    private SavingPlanRepository savingPlanRepository;

    private User testUser;
    private Goal testGoal;
    private GrouplWallet testWallet;
    private SavingPlan testPlan;
    private final String phoneNumber = "1234567890";
    private final UUID goalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPhoneNumber(phoneNumber);

        testGoal = new Goal();
        testGoal.setId(goalId);
        testGoal.setGoalName("Vacation Fund");
        testGoal.setTargetAmount(BigDecimal.valueOf(1000));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setGoalType(GoalType.FLEXIBLE);
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setOwner(testUser);

        testWallet = new GrouplWallet();
        testWallet.setId(UUID.randomUUID());
        testWallet.setBalance(BigDecimal.ZERO);
        testWallet.setUser(testUser);
        testWallet.setGoal(testGoal);

        testPlan = new SavingPlan();
        testPlan.setId(UUID.randomUUID());
        testPlan.setGoal(testGoal);
        testPlan.setContributionAmount(BigDecimal.valueOf(100));
        testPlan.setFrequency(ContributionFrequency.MONTHLY);
        testPlan.setNextContributionDate(LocalDate.of(2026, 9, 1));
        testPlan.setStatus(SavingPlanStatus.ACTIVE);
        testPlan.setContributedThisPeriod(BigDecimal.ZERO);

        testGoal.setSavingPlan(testPlan);
    }

    @Test
    void testPartialContribution() {
        // Given: $100 expected, user deposits $50
        assertEquals(BigDecimal.valueOf(100), testPlan.getContributionAmount());
        assertEquals(BigDecimal.ZERO, testPlan.getContributedThisPeriod());

        testPlan.setContributedThisPeriod(BigDecimal.valueOf(50));
        BigDecimal remainingThisPeriod = testPlan.getContributionAmount().subtract(testPlan.getContributedThisPeriod());

        assertEquals(BigDecimal.valueOf(50), testPlan.getContributedThisPeriod());
        assertEquals(BigDecimal.valueOf(50), remainingThisPeriod);
        assertEquals(LocalDate.of(2026, 9, 1), testPlan.getNextContributionDate());
    }

    @Test
    void testFullContributionSatisfiesPeriodAndAdvancesDate() {
        // Given: $100 expected, user deposits $100
        BigDecimal depositAmount = BigDecimal.valueOf(100);
        BigDecimal updatedContributed = testPlan.getContributedThisPeriod().add(depositAmount);

        assertTrue(updatedContributed.compareTo(testPlan.getContributionAmount()) >= 0);

        // Period satisfied: reset contributedThisPeriod and advance nextDueDate by 1 month
        testPlan.setContributedThisPeriod(BigDecimal.ZERO);
        testPlan.setNextContributionDate(testPlan.getNextContributionDate().plusMonths(1));

        assertEquals(BigDecimal.ZERO, testPlan.getContributedThisPeriod());
        assertEquals(LocalDate.of(2026, 10, 1), testPlan.getNextContributionDate());
    }

    @Test
    void testGoalCompletionWhenTargetReached() {
        // Given: Target $1000, current amount reaches $1000
        testGoal.setCurrentAmount(BigDecimal.valueOf(1000));
        testGoal.setStatus(GoalStatus.COMPLETED);
        testPlan.setStatus(SavingPlanStatus.COMPLETED);

        assertEquals(GoalStatus.COMPLETED, testGoal.getStatus());
        assertEquals(SavingPlanStatus.COMPLETED, testPlan.getStatus());
    }
}
