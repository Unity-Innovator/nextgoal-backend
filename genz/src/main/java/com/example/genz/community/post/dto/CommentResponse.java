package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.CommentStatus;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID parentCommentId,
        CommentAuthorResponse author,
        String content,
        CommentStatus status,
        int likeCount,
        int replyCount,
        boolean likedByCurrentUser,
        boolean ownedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
}
