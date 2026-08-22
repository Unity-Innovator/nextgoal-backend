package com.example.genz.community.notification.service;

import com.example.genz.community.notification.event.CommunityNotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityNotificationRealtimePublisher {

    public static final String NOTIFICATION_QUEUE = "/queue/community/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(CommunityNotificationCreatedEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(
                    event.recipientUsername(),
                    NOTIFICATION_QUEUE,
                    event.notification()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Could not deliver community notification {} to the WebSocket user queue",
                    event.notification().id(),
                    exception
            );
        }
    }
}
