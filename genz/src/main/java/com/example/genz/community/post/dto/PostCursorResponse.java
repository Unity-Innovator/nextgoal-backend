package com.example.genz.community.post.dto;

import java.time.Instant;
import java.util.UUID;

public record PostCursorResponse(
        Instant createdAt,
        UUID postId
) {
}
