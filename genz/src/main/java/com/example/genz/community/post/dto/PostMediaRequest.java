package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PostMediaRequest(
        @NotNull PostMediaType mediaType,
        @NotBlank @Size(max = 2048) String mediaUrl,
        @Size(max = 2048) String thumbnailUrl,
        @Positive Integer widthPx,
        @Positive Integer heightPx,
        @PositiveOrZero Integer durationMs
) {
}
