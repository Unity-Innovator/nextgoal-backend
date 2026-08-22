package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostMediaType;

import java.time.Instant;
import java.util.UUID;

public record PostMediaResponse(
        UUID id,
        PostMediaType mediaType,
        String mediaUrl,
        String thumbnailUrl,
        Integer widthPx,
        Integer heightPx,
        Integer durationMs,
        short sortOrder,
        Instant createdAt
) {
}
