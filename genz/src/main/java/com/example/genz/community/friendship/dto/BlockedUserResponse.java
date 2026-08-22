package com.example.genz.community.friendship.dto;

import java.time.Instant;
import java.util.UUID;

public record BlockedUserResponse(
        UUID blockId,
        FriendshipUserResponse user,
        Instant blockedAt
) {
}
