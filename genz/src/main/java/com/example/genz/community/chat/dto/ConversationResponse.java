package com.example.genz.community.chat.dto;

import com.example.genz.community.chat.enums.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        String title,
        String imageUrl,
        List<ConversationMemberResponse> members,
        MessageSummaryResponse lastMessage,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt
) {
}
