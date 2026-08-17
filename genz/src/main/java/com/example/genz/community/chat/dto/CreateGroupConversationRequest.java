package com.example.genz.community.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateGroupConversationRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2_000) String imageUrl,
        @NotEmpty @Size(max = 99) Set<UUID> memberIds
) {
}
