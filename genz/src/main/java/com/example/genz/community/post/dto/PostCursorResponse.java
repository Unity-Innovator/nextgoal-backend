package com.example.genz.community.post.dto;

import java.time.Instant;
import java.util.UUID;

public record PostCursorResponse(
        Instant createdAt,
        UUID postId,
        UUID randomSeed,
        Instant snapshot
) {
    public PostCursorResponse(Instant createdAt, UUID postId) {
        this(createdAt, postId, null, null);
    }
}
