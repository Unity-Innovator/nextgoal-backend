package com.example.genz.community.friendship.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendFriendRequestRequest(
        @NotNull(message = "userId is required") UUID userId
) {
}
