package com.example.genz;

import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Enums.GoalCategory;
import com.example.genz.Enums.GoalStatus;
import com.example.genz.Enums.GoalType;
import com.example.genz.Enums.MemberRole;
import com.example.genz.Enums.MemberStatus;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalMemberRepository;
import com.example.genz.Repository.GoalRepository.GoalRepository;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.Service.GoalService.GoalServiceImpl;
import com.example.genz.exception.ResourceNoteFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetGoalEndpointTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMemberRepository goalMemberRepository;

    @Mock
    private GoalWalletRepository goalWalletRepository;

    @Mock
    private com.example.genz.Repository.GoalRepository.SavingPlanRepository savingPlanRepository;

    @Mock
    private com.example.genz.Repository.GoalRepository.GoalContributionRepository goalContributionRepository;

    @InjectMocks
    private GoalServiceImpl goalService;

    private User userOwner;
    private User userMember;
    private User userNonMember;
    private Goal testGoal;
    private final UUID goalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userOwner = new User();
        userOwner.setId(UUID.randomUUID());
        userOwner.setFullName("John Owner");
        userOwner.setPhoneNumber("0900000001");

        userMember = new User();
        userMember.setId(UUID.randomUUID());
        userMember.setFullName("Alex Member");
        userMember.setPhoneNumber("0900000002");

        userNonMember = new User();
        userNonMember.setId(UUID.randomUUID());
        userNonMember.setFullName("Charlie NonMember");
        userNonMember.setPhoneNumber("0900000003");

        testGoal = new Goal();
        testGoal.setId(goalId);
        testGoal.setGoalName("Vacation Fund");
        testGoal.setTargetAmount(BigDecimal.valueOf(1000));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setGoalType(GoalType.FLEXIBLE);
        testGoal.setCategory(GoalCategory.TRAVEL);
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setOwner(userOwner);
        testGoal.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void test1_GoalOwnerCanAccessGoal() {
        when(userRepository.findByPhoneNumber(userOwner.getPhoneNumber())).thenReturn(Optional.of(userOwner));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GoalResponse response = goalService.getGoal(goalId, userOwner.getPhoneNumber());

        assertNotNull(response);
        assertEquals(goalId, response.getGoalId());
        assertEquals("Vacation Fund", response.getGoalName());
        assertNotNull(response.getOwner());
        assertEquals(userOwner.getId(), response.getOwner().getUserId());
        assertEquals("John Owner", response.getOwner().getName());
    }

    @Test
    void test2_ActiveGoalMemberCanAccessGoal() {
        when(userRepository.findByPhoneNumber(userMember.getPhoneNumber())).thenReturn(Optional.of(userMember));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalMemberRepository.existsByGoalIdAndUserIdAndStatus(goalId, userMember.getId(), MemberStatus.ACTIVE))
                .thenReturn(true);

        GoalResponse response = goalService.getGoal(goalId, userMember.getPhoneNumber());

        assertNotNull(response);
        assertEquals(goalId, response.getGoalId());
    }

    @Test
    void test3_NonMemberCannotAccessGoal() {
        when(userRepository.findByPhoneNumber(userNonMember.getPhoneNumber())).thenReturn(Optional.of(userNonMember));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));
        when(goalMemberRepository.existsByGoalIdAndUserIdAndStatus(goalId, userNonMember.getId(), MemberStatus.ACTIVE))
                .thenReturn(false);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                goalService.getGoal(goalId, userNonMember.getPhoneNumber())
        );

        assertEquals("You do not have access to this goal", exception.getMessage());
    }

    @Test
    void test4_NonexistentGoalThrowsNotFound() {
        UUID invalidGoalId = UUID.randomUUID();
        when(userRepository.findByPhoneNumber(userOwner.getPhoneNumber())).thenReturn(Optional.of(userOwner));
        when(goalRepository.findById(invalidGoalId)).thenReturn(Optional.empty());

        ResourceNoteFoundException exception = assertThrows(ResourceNoteFoundException.class, () ->
                goalService.getGoal(invalidGoalId, userOwner.getPhoneNumber())
        );

        assertEquals("Goal not found", exception.getMessage());
    }

    @Test
    void test5_GoalWithMultipleMembersReturnsMemberList() {
        when(userRepository.findByPhoneNumber(userOwner.getPhoneNumber())).thenReturn(Optional.of(userOwner));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GoalMember ownerMember = new GoalMember();
        ownerMember.setGoal(testGoal);
        ownerMember.setUser(userOwner);
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setStatus(MemberStatus.ACTIVE);

        GoalMember activeMember = new GoalMember();
        activeMember.setGoal(testGoal);
        activeMember.setUser(userMember);
        activeMember.setRole(MemberRole.MEMBER);
        activeMember.setStatus(MemberStatus.ACTIVE);

        when(goalMemberRepository.findByGoalAndStatusWithUser(testGoal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, activeMember));

        GoalResponse response = goalService.getGoal(goalId, userOwner.getPhoneNumber());

        assertNotNull(response.getMembers());
        assertEquals(2, response.getMembers().size());

        GoalResponse.MemberInfo member1 = response.getMembers().get(0);
        assertEquals(userOwner.getId(), member1.getUserId());
        assertEquals("John Owner", member1.getName());
        assertEquals(MemberRole.OWNER, member1.getRole());

        GoalResponse.MemberInfo member2 = response.getMembers().get(1);
        assertEquals(userMember.getId(), member2.getUserId());
        assertEquals("Alex Member", member2.getName());
        assertEquals(MemberRole.MEMBER, member2.getRole());
    }

    @Test
    void test6_GoalWithWalletBalanceCalculatesAmountsAndProgressCorrectly() {
        when(userRepository.findByPhoneNumber(userOwner.getPhoneNumber())).thenReturn(Optional.of(userOwner));
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(testGoal));

        GrouplWallet wallet = new GrouplWallet();
        wallet.setGoal(testGoal);
        wallet.setUser(userOwner);
        wallet.setBalance(BigDecimal.valueOf(350));

        when(goalWalletRepository.findByGoal(testGoal)).thenReturn(Optional.of(wallet));

        GoalResponse response = goalService.getGoal(goalId, userOwner.getPhoneNumber());

        assertEquals(BigDecimal.valueOf(1000), response.getTargetAmount());
        assertEquals(BigDecimal.valueOf(350), response.getCurrentAmount());
        assertEquals(BigDecimal.valueOf(650), response.getRemainingAmount());
        assertEquals(35, response.getProgressPercentage());
    }
}
