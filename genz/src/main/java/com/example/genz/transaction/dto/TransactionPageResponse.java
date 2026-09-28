package com.example.genz.transaction.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record TransactionPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> TransactionPageResponse<T> from(Page<T> page) {
        return new TransactionPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
