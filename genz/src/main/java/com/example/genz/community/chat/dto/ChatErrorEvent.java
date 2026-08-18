package com.example.genz.community.chat.dto;

import java.time.Instant;

public record ChatErrorEvent(
        String event,
        String code,
        String message,
        Instant timestamp
) {
}
