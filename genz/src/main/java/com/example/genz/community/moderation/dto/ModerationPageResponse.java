package com.example.genz.community.moderation.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ModerationPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> ModerationPageResponse<T> from(Page<T> page) {
        return new ModerationPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
