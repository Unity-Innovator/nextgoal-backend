package com.example.genz.community.moderation.dto;

import java.util.UUID;

public record ModerationUserResponse(
        UUID id,
        String fullName
) {
}
