package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        PostAuthorResponse author,
        UUID goalId,
        UUID sharedPostId,
        PostType postType,
        String content,
        PostVisibility visibility,
        PostStatus status,
        boolean pinned,
        boolean ownedByCurrentUser,
        boolean likedByCurrentUser,
        List<PostMediaResponse> media,
        int likeCount,
        int commentCount,
        int shareCount,
        Instant createdAt,
        Instant updatedAt
) {
}
