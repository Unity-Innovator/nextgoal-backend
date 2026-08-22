package com.example.genz.community.friendship.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record FriendshipPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> FriendshipPageResponse<T> from(Page<T> page) {
        return new FriendshipPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
