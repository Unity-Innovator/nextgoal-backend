package com.example.genz.community.chat.dto;

import com.example.genz.community.chat.enums.MessageStatus;
import com.example.genz.community.chat.enums.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageSummaryResponse(
        UUID id,
        UUID senderId,
        MessageType type,
        String body,
        MessageStatus status,
        Instant createdAt
) {
}
