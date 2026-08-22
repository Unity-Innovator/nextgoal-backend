package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(max = 5000) String content,
        PostVisibility visibility,
        @Size(max = 10) List<@Valid PostMediaRequest> media
) {
}
