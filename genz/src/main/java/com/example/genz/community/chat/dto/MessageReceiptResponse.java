package com.example.genz.community.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageReceiptResponse(
        UUID userId,
        Instant deliveredAt,
        Instant readAt
) {
}
