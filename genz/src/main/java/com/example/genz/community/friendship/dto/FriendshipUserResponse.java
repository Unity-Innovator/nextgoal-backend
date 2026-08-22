package com.example.genz.community.friendship.dto;

import java.util.UUID;

public record FriendshipUserResponse(
        UUID id,
        String fullName
) {
}
