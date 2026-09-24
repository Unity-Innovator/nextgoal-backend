package com.example.genz.notification.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.service.ChallengeCompletionService;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.invitation.dto.CreateInvitationRequest;
import com.example.genz.invitation.dto.InvitationResponse;
import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import com.example.genz.invitation.mapper.GoalInvitationMapper;
import com.example.genz.invitation.repository.GoalInvitationRepository;
import com.example.genz.invitation.service.GoalInvitationServiceImpl;
import com.example.genz.notification.dto.NotificationResponse;
import com.example.genz.notification.entity.Notification;
import com.example.genz.notification.enums.NotificationType;
import com.example.genz.notification.mapper.NotificationMapper;
import com.example.genz.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalInvitationNotificationRealtimeFlowTests {

    @Mock
    private GoalInvitationRepository goalInvitationRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalMemberRepository goalMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationWebSocketPublisher notificationWebSocketPublisher;
    @Mock
    private ChallengeCompletionService challengeCompletionService;

    @Spy
    private final NotificationMapper notificationMapper = new NotificationMapper();
    @Spy
    private final GoalInvitationMapper goalInvitationMapper = new GoalInvitationMapper();

    private NotificationServiceImpl notificationService;
    private GoalInvitationServiceImpl goalInvitationService;

    private User userA; // Inviter / Owner
    private User userB; // Invitee
    private User userC; // Other user
    private Goal goal;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                goalMemberRepository,
                notificationMapper,
                notificationWebSocketPublisher
        );

        goalInvitationService = new GoalInvitationServiceImpl(
                goalInvitationRepository,
                goalRepository,
                goalMemberRepository,
                userRepository,
                notificationService,
                goalInvitationMapper,
                challengeCompletionService
        );

        userA = new User();
        userA.setId(UUID.randomUUID());
        userA.setFullName("User A");
        userA.setPhoneNumber("0900000001");

        userB = new User();
        userB.setId(UUID.randomUUID());
        userB.setFullName("User B");
        userB.setPhoneNumber("0900000002");

        userC = new User();
        userC.setId(UUID.randomUUID());
        userC.setFullName("User C");
        userC.setPhoneNumber("0900000003");

        goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setGoalName("Tokyo Trip");
        goal.setTargetAmount(BigDecimal.valueOf(5000));
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setOwner(userA);
    }

    @Test
    @DisplayName("Scenario 1: User A invites User B -> Invitation saved, Notification saved, WebSocket delivered to User B")
    void testUserAInvitesUserB_WebSocketDeliveredToUserB() {
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setInviteeId(userB.getId());

        when(userRepository.findByPhoneNumber("0900000001")).thenReturn(Optional.of(userA));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), userB.getId())).thenReturn(false);
        when(goalInvitationRepository.existsByGoalIdAndInviteeIdAndStatus(goal.getId(), userB.getId(), InvitationStatus.PENDING)).thenReturn(false);

        UUID invitationId = UUID.randomUUID();
        when(goalInvitationRepository.save(any(GoalInvitation.class))).thenAnswer(invocation -> {
            GoalInvitation gi = invocation.getArgument(0);
            gi.setId(invitationId);
            gi.setCreatedAt(LocalDateTime.now());
            return gi;
        });

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        InvitationResponse response = goalInvitationService.inviteUser(goal.getId(), request, userA.getPhoneNumber());

        assertNotNull(response);
        assertEquals(invitationId, response.getInvitationId());

        // Verify notification saved in PostgreSQL
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(userB.getId(), savedNotification.getUser().getId());
        assertEquals(NotificationType.GOAL_INVITATION, savedNotification.getType());
        assertEquals(invitationId, savedNotification.getReferenceId());
        assertFalse(savedNotification.isRead());

        // Verify WebSocket delivered to User B's phone number
        ArgumentCaptor<NotificationResponse> wsCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(notificationWebSocketPublisher, times(1)).sendToUser(eq(userB.getPhoneNumber()), wsCaptor.capture());

        NotificationResponse wsNotification = wsCaptor.getValue();
        assertEquals(NotificationType.GOAL_INVITATION.name(), wsNotification.getType());
        assertEquals(invitationId, wsNotification.getReferenceId());
        assertFalse(wsNotification.isRead());
    }

    @Test
    @DisplayName("Scenario 2: Offline User B receives notification via GET /api/notifications")
    void testOfflineUser_NotificationSavedAndRetrievableViaRest() {
        Notification notification = new Notification();
        UUID notifId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        notification.setId(notifId);
        notification.setUser(userB);
        notification.setTitle("Goal Invitation");
        notification.setMessage("User A invited you to join Tokyo Trip goal");
        notification.setType(NotificationType.GOAL_INVITATION);
        notification.setReferenceId(refId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByPhoneNumber(userB.getPhoneNumber())).thenReturn(Optional.of(userB));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userB.getId())).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getUserNotifications(userB.getPhoneNumber());

        assertEquals(1, responses.size());
        NotificationResponse res = responses.get(0);
        assertEquals(notifId, res.getId());
        assertEquals("GOAL_INVITATION", res.getType());
        assertEquals(refId, res.getReferenceId());
        assertFalse(res.isRead());
    }

    @Test
    @DisplayName("Scenario 3: Correct recipient - User C never receives User B's notification")
    void testRecipientIsolation_UserCDoesNotReceiveNotification() {
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setInviteeId(userB.getId());

        when(userRepository.findByPhoneNumber(userA.getPhoneNumber())).thenReturn(Optional.of(userA));
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(userRepository.findById(userB.getId())).thenReturn(Optional.of(userB));
        when(goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), userB.getId())).thenReturn(false);
        when(goalInvitationRepository.existsByGoalIdAndInviteeIdAndStatus(goal.getId(), userB.getId(), InvitationStatus.PENDING)).thenReturn(false);

        when(goalInvitationRepository.save(any(GoalInvitation.class))).thenAnswer(invocation -> {
            GoalInvitation gi = invocation.getArgument(0);
            gi.setId(UUID.randomUUID());
            return gi;
        });
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        goalInvitationService.inviteUser(goal.getId(), request, userA.getPhoneNumber());

        // Verify sent ONLY to User B, NOT to User C or User A
        verify(notificationWebSocketPublisher).sendToUser(eq(userB.getPhoneNumber()), any());
        verify(notificationWebSocketPublisher, never()).sendToUser(eq(userC.getPhoneNumber()), any());
        verify(notificationWebSocketPublisher, never()).sendToUser(eq(userA.getPhoneNumber()), any());
    }

    @Test
    @DisplayName("Scenario 4: Accept and Reject APIs mark notification as read and remain REST-driven")
    void testAcceptAndRejectMarkNotificationRead() {
        UUID invitationId = UUID.randomUUID();
        GoalInvitation invitation = new GoalInvitation();
        invitation.setId(invitationId);
        invitation.setGoal(goal);
        invitation.setInviter(userA);
        invitation.setInvitee(userB);
        invitation.setStatus(InvitationStatus.PENDING);

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(userB);
        notification.setReferenceId(invitationId);
        notification.setRead(false);

        when(userRepository.findByPhoneNumber(userB.getPhoneNumber())).thenReturn(Optional.of(userB));
        when(goalInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(goalInvitationRepository.save(any(GoalInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.findByReferenceIdAndUserId(invitationId, userB.getId())).thenReturn(Optional.of(notification));

        // Accept
        InvitationResponse acceptResponse = goalInvitationService.acceptInvitation(invitationId, userB.getPhoneNumber());
        assertEquals(InvitationStatus.ACCEPTED.name(), acceptResponse.getStatus());
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);

        // Reject setup
        invitation.setStatus(InvitationStatus.PENDING);
        notification.setRead(false);
        InvitationResponse rejectResponse = goalInvitationService.rejectInvitation(invitationId, userB.getPhoneNumber());
        assertEquals(InvitationStatus.REJECTED.name(), rejectResponse.getStatus());
        assertTrue(notification.isRead());
    }

    @Test
    @DisplayName("Scenario 5: NotificationResponse JSON serializes 'read' boolean field matching Flutter contract")
    void testNotificationResponseSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        NotificationResponse response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .title("New Goal Invitation")
                .message("Teng invited you to join a goal")
                .type("GOAL_INVITATION")
                .referenceId(UUID.randomUUID())
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 9, 24, 8, 2, 40))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"read\":false"));
        assertFalse(json.contains("\"isRead\""));

        // Verify deserialization works with both "read" and "isRead"
        NotificationResponse deserialized = objectMapper.readValue(json, NotificationResponse.class);
        assertFalse(deserialized.isRead());

        String jsonWithIsRead = json.replace("\"read\":false", "\"isRead\":false");
        NotificationResponse deserializedFromIsRead = objectMapper.readValue(jsonWithIsRead, NotificationResponse.class);
        assertFalse(deserializedFromIsRead.isRead());
    }
}
