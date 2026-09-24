package com.example.genz.notification.service;

import com.example.genz.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketPublisher {

    public static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String userIdentifier, NotificationResponse notification) {
        if (userIdentifier == null || userIdentifier.isBlank() || notification == null) {
            log.warn("Cannot send notification via WebSocket: invalid userIdentifier or notification is null");
            return;
        }

        Runnable sendTask = () -> {
            try {
                messagingTemplate.convertAndSendToUser(
                        userIdentifier,
                        NOTIFICATION_QUEUE,
                        notification
                );
                log.info("Delivered notification {} to user {} via WebSocket queue {}",
                        notification.getId(), userIdentifier, NOTIFICATION_QUEUE);
            } catch (Exception exception) {
                log.warn("Failed to deliver notification {} to user {} via WebSocket: {}",
                        notification.getId(), userIdentifier, exception.getMessage(), exception);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
        } else {
            sendTask.run();
        }
    }
}
