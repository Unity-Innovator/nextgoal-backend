package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @NotNull PostType postType,
        @Size(max = 5000) String content,
        PostVisibility visibility,
        UUID goalId,
        @Size(max = 10) List<@Valid PostMediaRequest> media
) {
}
