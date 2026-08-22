package com.example.genz.community.notification.service;

import com.example.genz.community.notification.event.CommunityNotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommunityNotificationEventListener {

    private final CommunityNotificationRealtimePublisher realtimePublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(CommunityNotificationCreatedEvent event) {
        realtimePublisher.publish(event);
    }
}
