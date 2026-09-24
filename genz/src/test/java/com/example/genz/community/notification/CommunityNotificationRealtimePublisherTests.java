package com.example.genz.community.notification;

import com.example.genz.community.notification.dto.CommunityNotificationResponse;
import com.example.genz.community.notification.enums.NotificationReferenceType;
import com.example.genz.community.notification.enums.NotificationType;
import com.example.genz.community.notification.event.CommunityNotificationCreatedEvent;
import com.example.genz.community.notification.service.CommunityNotificationRealtimePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommunityNotificationRealtimePublisherTests {

    @Test
    void publishesToExistingAuthenticatedWebSocketUserQueue() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        CommunityNotificationRealtimePublisher publisher =
                new CommunityNotificationRealtimePublisher(messagingTemplate);
        CommunityNotificationResponse notification = new CommunityNotificationResponse(
                UUID.randomUUID(),
                null,
                NotificationType.POST_LIKED,
                NotificationReferenceType.POST,
                UUID.randomUUID(),
                Map.of(),
                false,
                null,
                Instant.now()
        );
        CommunityNotificationCreatedEvent event = new CommunityNotificationCreatedEvent(
                "0152000001",
                notification
        );

        publisher.publish(event);

        verify(messagingTemplate).convertAndSendToUser(
                "0152000001",
                CommunityNotificationRealtimePublisher.NOTIFICATION_QUEUE,
                notification
        );
    }
}
