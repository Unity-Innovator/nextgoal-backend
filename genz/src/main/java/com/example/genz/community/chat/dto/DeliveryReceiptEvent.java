package com.example.genz.community.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryReceiptEvent(
        String event,
        UUID conversationId,
        UUID messageId,
        UUID userId,
        Instant deliveredAt
) {
}
