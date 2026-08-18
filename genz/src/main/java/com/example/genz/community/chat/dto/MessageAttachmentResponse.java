package com.example.genz.community.chat.dto;

import com.example.genz.community.chat.enums.AttachmentType;

import java.util.UUID;

public record MessageAttachmentResponse(
        UUID id,
        AttachmentType type,
        String fileUrl,
        String thumbnailUrl,
        String originalName,
        String mimeType,
        Long fileSizeBytes,
        Integer widthPx,
        Integer heightPx,
        Integer durationMs
) {
}
