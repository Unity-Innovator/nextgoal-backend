package com.example.genz.community.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationCursorResponse(
        Instant createdAt,
        UUID notificationId
) {
}
