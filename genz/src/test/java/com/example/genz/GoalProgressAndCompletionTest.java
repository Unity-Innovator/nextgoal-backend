package com.example.genz;

import com.example.genz.dto.GoalDTO.CreateGoalRequest;
import com.example.genz.dto.GoalDTO.GoalResponse;
import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Goal.Goal;
import com.example.genz.entity.Goal.GoalMember;
import com.example.genz.entity.Goal.SavingPlan;
import com.example.genz.entity.Transaction.GoalContribution;
import com.example.genz.enums.CompletionStatus;
import com.example.genz.enums.ContributionFrequency;
import com.example.genz.enums.GoalCategory;
import com.example.genz.enums.GoalStatus;
import com.example.genz.enums.GoalType;
import com.example.genz.enums.MemberRole;
import com.example.genz.enums.MemberStatus;
import com.example.genz.repository.AuthRepository.UserRepository;
import com.example.genz.repository.GoalRepository.GoalContributionRepository;
import com.example.genz.repository.GoalRepository.GoalMemberRepository;
import com.example.genz.repository.GoalRepository.GoalRepository;
import com.example.genz.repository.GoalRepository.SavingPlanRepository;
import com.example.genz.repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.service.GoalService.GoalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.genz.mapper.GoalMapper;
import com.example.genz.mapper.SavingPlanMapper;
import com.example.genz.mapper.WalletMapper;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class GoalProgressAndCompletionTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalWalletRepository goalWalletRepository;
    @Mock
    private GoalMemberRepository goalMemberRepository;
    @Mock
    private SavingPlanRepository savingPlanRepository;
    @Mock
    private GoalContributionRepository goalContributionRepository;
    @Spy
    private GoalMapper goalMapper = new GoalMapper();
    @Spy
    private SavingPlanMapper savingPlanMapper = new SavingPlanMapper();
    @Spy
    private WalletMapper walletMapper = new WalletMapper();

    @InjectMocks
    private GoalServiceImpl goalService;

    private User testUser;
    private Goal testGoal;
    private final String phoneNumber = "1234567890";
    private final UUID goalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPhoneNumber(phoneNumber);

        testGoal = new Goal();
        testGoal.setId(goalId);
        testGoal.setGoalName("Laptop Fund");
        testGoal.setTargetAmount(BigDecimal.valueOf(1000));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setGoalType(GoalType.FLEXIBLE);
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setOwner(testUser);
        testGoal.setCreatedAt(LocalDateTime.now().minusDays(30));
    }

    @Test
    void testZeroProgress() {
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertEquals(0, response.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(1000), response.getRemainingAmount());
    }

    @Test
    void testFiftyPercentProgress() {
        testGoal.setCurrentAmount(BigDecimal.valueOf(500));
        testGoal.setRemaining(BigDecimal.valueOf(500));

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertEquals(50, response.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(500), response.getRemainingAmount());
    }

    @Test
    void testHundredPercentProgress() {
        testGoal.setCurrentAmount(BigDecimal.valueOf(1000));
        testGoal.setRemaining(BigDecimal.ZERO);
        testGoal.setStatus(GoalStatus.COMPLETED);
        testGoal.setCompletedAt(LocalDateTime.now());

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertEquals(100, response.getProgressPercentage());
        assertEquals(BigDecimal.ZERO, response.getRemainingAmount());
        assertNotNull(response.getEstimatedCompletionDate());
    }

    @Test
    void testNoContributionHistoryReturnsNullEstimatedDate() {
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goalId)).thenReturn(Collections.emptyList());

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertNull(response.getEstimatedCompletionDate());
        assertNull(response.getCompletionStatus());
    }

    @Test
    void testAheadStatusWhenUserSavesFaster() {
        LocalDate targetDate = LocalDate.now().plusDays(100);
        testGoal.setTargetDate(targetDate);
        testGoal.setCurrentAmount(BigDecimal.valueOf(500));

        GoalMember member = new GoalMember();
        member.setUser(testUser);

        GoalContribution c1 = new GoalContribution();
        c1.setAmount(BigDecimal.valueOf(500));
        c1.setContributedAt(LocalDateTime.now().minusDays(10));

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goalId)).thenReturn(List.of(c1));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertNotNull(response.getEstimatedCompletionDate());
        assertEquals(targetDate, response.getExpectedCompletionDate());
        assertEquals(CompletionStatus.AHEAD, response.getCompletionStatus());
        assertTrue(response.getDaysDifference() > 7);
    }

    @Test
    void testBehindStatusWhenUserSavesSlower() {
        LocalDate targetDate = LocalDate.now().plusDays(10);
        testGoal.setTargetDate(targetDate);
        testGoal.setCurrentAmount(BigDecimal.valueOf(100));

        GoalContribution c1 = new GoalContribution();
        c1.setAmount(BigDecimal.valueOf(100));
        c1.setContributedAt(LocalDateTime.now().minusDays(20));

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goalId)).thenReturn(List.of(c1));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertNotNull(response.getEstimatedCompletionDate());
        assertEquals(CompletionStatus.BEHIND, response.getCompletionStatus());
    }

    @Test
    void testOnTrackStatusWithinTolerance() {
        LocalDate targetDate = LocalDate.now().plusDays(20);
        testGoal.setTargetDate(targetDate);
        testGoal.setCurrentAmount(BigDecimal.valueOf(500));

        GoalContribution c1 = new GoalContribution();
        c1.setAmount(BigDecimal.valueOf(500));
        c1.setContributedAt(LocalDateTime.now().minusDays(20));

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goalId)).thenReturn(List.of(c1));

        GoalResponse response = goalService.getGoal(goalId, phoneNumber);

        assertEquals(CompletionStatus.ON_TRACK, response.getCompletionStatus());
    }
}
