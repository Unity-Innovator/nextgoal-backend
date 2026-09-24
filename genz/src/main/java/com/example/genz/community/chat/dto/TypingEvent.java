package com.example.genz.community.chat.dto;

import java.util.UUID;

public record TypingEvent(
        String event,
        UUID conversationId,
        UUID userId,
        boolean typing
) {
}
