package com.example.genz.community.chat.dto;

import java.util.UUID;

public record ChatUserResponse(
        UUID id,
        String fullName,
        String avatarUrl
) {
}
