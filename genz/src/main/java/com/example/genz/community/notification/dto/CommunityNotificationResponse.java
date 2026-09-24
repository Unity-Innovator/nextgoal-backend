package com.example.genz.community.notification.dto;

import com.example.genz.community.notification.enums.NotificationReferenceType;
import com.example.genz.community.notification.enums.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CommunityNotificationResponse(
        UUID id,
        NotificationActorResponse actor,
        NotificationType type,
        NotificationReferenceType referenceType,
        UUID referenceId,
        Map<String, Object> payload,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
