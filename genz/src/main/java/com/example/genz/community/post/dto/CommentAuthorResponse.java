package com.example.genz.community.post.dto;

import java.util.UUID;

public record CommentAuthorResponse(
        UUID id,
        String fullName
) {
}
