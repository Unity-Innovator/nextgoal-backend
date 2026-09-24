package com.example.genz.community.post.dto;

import com.example.genz.community.post.enums.PostMediaType;

public record PostMediaUploadResponse(
        PostMediaType mediaType,
        String mediaUrl,
        String originalName,
        String mimeType,
        long fileSizeBytes
) {
}
