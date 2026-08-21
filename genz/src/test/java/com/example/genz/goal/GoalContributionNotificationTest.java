package com.example.genz.goal;

import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.notification.entity.Notification;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.notification.enums.NotificationType;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.repository.SavingPlanRepository;
import com.example.genz.notification.repository.NotificationRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.notification.service.NotificationServiceImpl;
import com.example.genz.wallet.service.WalletServiceImpl;
import com.example.genz.wallet.mapper.WalletMapper;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.genz.notification.mapper.NotificationMapper;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class GoalContributionNotificationTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GoalMemberRepository goalMemberRepository;
    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalWalletRepository goalWalletRepository;
    @Mock
    private GoalContributionRepository goalContributionRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private SavingPlanRepository savingPlanRepository;
    @Mock
    private NotificationServiceImpl mockNotificationService;

    private WalletServiceImpl walletService;

    private User owner;
    private User memberAlex;
    private User memberDavid;
    private User pendingUser;
    private User leftUser;

    private Goal goal;
    private GoalMember ownerMember;
    private GoalMember alexMember;
    private GoalMember davidMember;
    private GoalMember pendingMember;
    private GoalMember leftMember;

    private GrouplWallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(
                userRepository,
                goalRepository,
                goalMemberRepository,
                goalWalletRepository,
                goalContributionRepository,
                walletTransactionRepository,
                savingPlanRepository,
                mockNotificationService,
                new WalletMapper()
        );

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFullName("John");
        owner.setPhoneNumber("0900000001");

        memberAlex = new User();
        memberAlex.setId(UUID.randomUUID());
        memberAlex.setFullName("Alex");
        memberAlex.setPhoneNumber("0900000002");

        memberDavid = new User();
        memberDavid.setId(UUID.randomUUID());
        memberDavid.setFullName("David");
        memberDavid.setPhoneNumber("0900000003");

        pendingUser = new User();
        pendingUser.setId(UUID.randomUUID());
        pendingUser.setFullName("Sarah (Pending)");
        pendingUser.setPhoneNumber("0900000004");

        leftUser = new User();
        leftUser.setId(UUID.randomUUID());
        leftUser.setFullName("Tom (Left)");
        leftUser.setPhoneNumber("0900000005");

        goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setGoalName("Vacation Fund");
        goal.setTargetAmount(BigDecimal.valueOf(1000));
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setOwner(owner);

        wallet = new GrouplWallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setGoal(goal);

        ownerMember = new GoalMember(UUID.randomUUID(), goal, owner, MemberRole.OWNER, MemberStatus.ACTIVE, null, null);
        alexMember = new GoalMember(UUID.randomUUID(), goal, memberAlex, MemberRole.MEMBER, MemberStatus.ACTIVE, null, null);
        davidMember = new GoalMember(UUID.randomUUID(), goal, memberDavid, MemberRole.MEMBER, MemberStatus.ACTIVE, null, null);
        pendingMember = new GoalMember(UUID.randomUUID(), goal, pendingUser, MemberRole.MEMBER, MemberStatus.INVITED, null, null);
        leftMember = new GoalMember(UUID.randomUUID(), goal, leftUser, MemberRole.MEMBER, MemberStatus.LEFT, null, null);
    }

    @Test
    @DisplayName("1. Owner contributes -> all members including owner receive notification")
    void testOwnerContributes_AllActiveMembersNotify() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(200));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember, davidMember));

        notificationService.createGoalContributionNotifications(goal, contribution, owner);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(notificationCaptor.capture());

        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertEquals(3, savedNotifications.size());

        List<UUID> recipientIds = savedNotifications.stream().map(n -> n.getUser().getId()).toList();
        assertTrue(recipientIds.contains(owner.getId()));
        assertTrue(recipientIds.contains(memberAlex.getId()));
        assertTrue(recipientIds.contains(memberDavid.getId()));

        Notification first = savedNotifications.get(0);
        assertEquals("New Contribution", first.getTitle());
        assertEquals("John contributed $200 to Vacation Fund.", first.getMessage());
        assertEquals(NotificationType.GOAL_CONTRIBUTION, first.getType());
        assertEquals(contribution.getId(), first.getReferenceId());
    }

    @Test
    @DisplayName("2. Member contributes -> all members including contributor receive notification")
    void testMemberContributes_AllActiveMembersNotify() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember, davidMember));

        notificationService.createGoalContributionNotifications(goal, contribution, memberAlex);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(notificationCaptor.capture());

        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertEquals(3, savedNotifications.size());

        List<UUID> recipientIds = savedNotifications.stream().map(n -> n.getUser().getId()).toList();
        assertTrue(recipientIds.contains(memberAlex.getId()));
        assertTrue(recipientIds.contains(owner.getId()));
        assertTrue(recipientIds.contains(memberDavid.getId()));

        Notification sample = savedNotifications.get(0);
        assertEquals("Alex contributed $100 to Vacation Fund.", sample.getMessage());
    }

    @Test
    @DisplayName("3. Multiple members contribute -> each contribution generates its own notifications")
    void testMultipleContributions_GenerateSeparateNotifications() {
        GoalContribution c1 = new GoalContribution();
        c1.setId(UUID.randomUUID());
        c1.setGoal(goal);
        c1.setAmount(BigDecimal.valueOf(50));

        GoalContribution c2 = new GoalContribution();
        c2.setId(UUID.randomUUID());
        c2.setGoal(goal);
        c2.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember));

        notificationService.createGoalContributionNotifications(goal, c1, memberAlex);
        notificationService.createGoalContributionNotifications(goal, c2, memberDavid);

        // c1 notifies owner & Alex (2); c2 notifies owner, Alex & contributor David (3) -> total 5
        verify(notificationRepository, times(5)).save(any(Notification.class));
    }

    @Test
    @DisplayName("4. User is not a goal member -> contribution rejected, 0 notifications created")
    void testNonMemberContribution_RejectedNoNotification() {
        User outsider = new User();
        outsider.setId(UUID.randomUUID());
        outsider.setPhoneNumber("0999999999");

        when(userRepository.findByPhoneNumber(outsider.getPhoneNumber())).thenReturn(Optional.of(outsider));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), outsider.getId())).thenReturn(false);

        DepositRequest request = new DepositRequest(goal.getId(), BigDecimal.valueOf(100));

        assertThrows(BadRequestException.class, () -> walletService.deposit(request, outsider.getPhoneNumber()));
        verify(mockNotificationService, never()).createGoalContributionNotifications(any(), any(), any());
    }

    @Test
    @DisplayName("5. Pending invitation user -> does not receive contribution notifications")
    void testPendingInvitationUser_NotNotified() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember));

        notificationService.createGoalContributionNotifications(goal, contribution, memberAlex);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        List<UUID> recipientIds = notificationCaptor.getAllValues().stream().map(n -> n.getUser().getId()).toList();
        assertFalse(recipientIds.contains(pendingUser.getId()));
    }

    @Test
    @DisplayName("6. Removed/inactive member -> does not receive notifications")
    void testLeftMember_NotNotified() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember));

        notificationService.createGoalContributionNotifications(goal, contribution, memberAlex);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        List<UUID> recipientIds = notificationCaptor.getAllValues().stream().map(n -> n.getUser().getId()).toList();
        assertFalse(recipientIds.contains(leftUser.getId()));
    }

    @Test
    @DisplayName("7. Contribution fails -> no notifications created")
    void testContributionFails_NoNotificationCreated() {
        when(userRepository.findByPhoneNumber(memberAlex.getPhoneNumber())).thenReturn(Optional.of(memberAlex));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        DepositRequest request = new DepositRequest(goal.getId(), BigDecimal.valueOf(100));

        assertThrows(ResourceNoteFoundException.class, () -> walletService.deposit(request, memberAlex.getPhoneNumber()));
        verify(mockNotificationService, never()).createGoalContributionNotifications(any(), any(), any());
    }

    @Test
    @DisplayName("8. Wallet update fails -> no notifications created")
    void testWalletUpdateFails_NoNotificationCreated() {
        when(userRepository.findByPhoneNumber(memberAlex.getPhoneNumber())).thenReturn(Optional.of(memberAlex));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), memberAlex.getId())).thenReturn(true);
        when(goalWalletRepository.findByGoal(goal)).thenReturn(Optional.empty());

        DepositRequest request = new DepositRequest(goal.getId(), BigDecimal.valueOf(100));

        assertThrows(ResourceNoteFoundException.class, () -> walletService.deposit(request, memberAlex.getPhoneNumber()));
        verify(mockNotificationService, never()).createGoalContributionNotifications(any(), any(), any());
    }

    @Test
    @DisplayName("9. Multiple members -> exactly one notification per recipient per contribution")
    void testDuplicatePrevention_OneNotificationPerRecipient() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, alexMember, davidMember));

        notificationService.createGoalContributionNotifications(goal, contribution, memberAlex);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());

        List<Notification> notifications = captor.getAllValues();
        long uniqueUserCount = notifications.stream().map(n -> n.getUser().getId()).distinct().count();
        assertEquals(3, uniqueUserCount, "Each recipient must receive exactly 1 notification");
    }

    @Test
    @DisplayName("10. Contributor receives notification -> contributor is present in recipient list")
    void testContributorReceivesNotification() {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoal(goal);
        contribution.setAmount(BigDecimal.valueOf(100));

        when(goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, davidMember));

        notificationService.createGoalContributionNotifications(goal, contribution, memberAlex);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());

        List<UUID> recipientIds = captor.getAllValues().stream().map(n -> n.getUser().getId()).toList();
        assertTrue(recipientIds.contains(memberAlex.getId()), "Contributor Alex must receive notification even if not in members list");
    }
}
