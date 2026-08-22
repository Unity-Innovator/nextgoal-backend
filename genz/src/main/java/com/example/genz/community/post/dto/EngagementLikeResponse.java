package com.example.genz.community.post.dto;

public record EngagementLikeResponse(
        boolean liked,
        boolean changed,
        int likeCount
) {
}
