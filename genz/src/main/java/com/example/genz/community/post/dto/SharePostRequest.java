package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostVisibility;
import jakarta.validation.constraints.Size;

public record SharePostRequest(
        @Size(max = 5000) String content,
        PostVisibility visibility
) {
}
