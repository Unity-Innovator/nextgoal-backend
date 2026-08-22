package com.example.genz.community.friendship.dto;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
        UUID friendshipId,
        FriendshipUserResponse user,
        Instant friendsSince
) {
}
