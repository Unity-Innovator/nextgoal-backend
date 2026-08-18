package com.example.genz.community.chat.dto;

import com.example.genz.community.chat.enums.MessageStatus;
import com.example.genz.community.chat.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID clientMessageId,
        ChatUserResponse sender,
        UUID replyToMessageId,
        MessageType type,
        String body,
        Map<String, Object> metadata,
        MessageStatus status,
        List<MessageAttachmentResponse> attachments,
        List<MessageReceiptResponse> receipts,
        Instant createdAt,
        Instant editedAt,
        Instant deletedAt
) {
}
