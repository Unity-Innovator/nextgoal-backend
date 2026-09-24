package com.example.genz.community.notification.dto;

import java.util.UUID;

public record NotificationActorResponse(
        UUID id,
        String fullName
) {
}
