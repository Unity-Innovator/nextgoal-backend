package com.example.genz.community.post.dto;

import java.util.List;

public record PostCursorPageResponse(
        List<PostResponse> items,
        PostCursorResponse nextCursor,
        boolean hasNext
) {
}
