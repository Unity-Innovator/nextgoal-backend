package com.example.genz.community.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 10_000) String body,
        UUID replyToMessageId
) {
}
