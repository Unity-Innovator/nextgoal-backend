package com.example.genz.community.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkConversationReadRequest(
        @NotNull UUID messageId
) {
}
