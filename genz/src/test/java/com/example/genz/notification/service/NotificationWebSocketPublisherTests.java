package com.example.genz.notification.service;

import com.example.genz.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketPublisherTests {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NotificationWebSocketPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("Publishes notification to existing STOMP user queue with phone number as principal identifier")
    void testPublishesToUserQueue() {
        UUID id = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        NotificationResponse notification = NotificationResponse.builder()
                .id(id)
                .title("Goal Invitation")
                .message("Alice invited you to join goal")
                .type("GOAL_INVITATION")
                .referenceId(refId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        publisher.sendToUser("0912345678", notification);

        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("0912345678"),
                eq(NotificationWebSocketPublisher.NOTIFICATION_QUEUE),
                captor.capture()
        );

        NotificationResponse sent = captor.getValue();
        assertEquals(id, sent.getId());
        assertEquals("GOAL_INVITATION", sent.getType());
        assertEquals(refId, sent.getReferenceId());
    }

    @Test
    @DisplayName("Ignores delivery gracefully when user identifier is null or empty")
    void testIgnoresNullOrBlankUserIdentifier() {
        NotificationResponse notification = NotificationResponse.builder().id(UUID.randomUUID()).build();

        assertDoesNotThrow(() -> publisher.sendToUser(null, notification));
        assertDoesNotThrow(() -> publisher.sendToUser("   ", notification));

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("Ignores delivery gracefully when notification is null")
    void testIgnoresNullNotification() {
        assertDoesNotThrow(() -> publisher.sendToUser("0912345678", null));

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("Catches and suppresses WebSocket messaging errors without failing")
    void testSuppressesWebSocketException() {
        NotificationResponse notification = NotificationResponse.builder().id(UUID.randomUUID()).build();

        doThrow(new MessagingException("Broker unavailable"))
                .when(messagingTemplate)
                .convertAndSendToUser(eq("0912345678"), eq(NotificationWebSocketPublisher.NOTIFICATION_QUEUE), any());

        assertDoesNotThrow(() -> publisher.sendToUser("0912345678", notification));
    }
}
