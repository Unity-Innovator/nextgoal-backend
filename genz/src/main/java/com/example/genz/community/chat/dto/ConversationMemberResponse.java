package com.example.genz.community.chat.dto;

import com.example.genz.community.chat.enums.ConversationMemberRole;

import java.time.Instant;

public record ConversationMemberResponse(
        ChatUserResponse user,
        ConversationMemberRole role,
        Instant joinedAt
) {
}
