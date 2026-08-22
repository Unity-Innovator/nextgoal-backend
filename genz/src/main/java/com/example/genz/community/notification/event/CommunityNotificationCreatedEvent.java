package com.example.genz.community.notification.event;

import com.example.genz.community.notification.dto.CommunityNotificationResponse;

public record CommunityNotificationCreatedEvent(
        String recipientUsername,
        CommunityNotificationResponse notification
) {
}
