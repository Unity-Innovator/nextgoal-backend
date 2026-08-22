package com.example.genz.community.friendship.dto;

import com.example.genz.community.friendship.enums.FriendRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponse(
        UUID id,
        FriendshipUserResponse sender,
        FriendshipUserResponse receiver,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
